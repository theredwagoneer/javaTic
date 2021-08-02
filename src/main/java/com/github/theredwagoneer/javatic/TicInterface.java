package com.github.theredwagoneer.javatic;

import java.io.File;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;


/**
 * The TicInterface class interfaces directly to the tic over USB.
 * The primary interface allows the setting of position based on pulses.
 * Calling classes will need to understand what pulse position means
 * in terms of real angle and/or position.
 * 
 * @author theredwagoneer
 *
 */
public class TicInterface {
	/**
	 * These are the 5 models of TIC and their product codes
	 */
	static public enum TicModel{
    	// I actually only know what the T834 is because I have one.
    	// TODO Maybe someone else can fill in the others.
    	TIC_T500(0x0),
    	TIC_T834(0xB5),
    	TIC_T825(0x0),
    	TIC_T249(0x0),
    	TIC_36v4(0x0);
    	
    	protected int code;
    	
    	TicModel(int code)
    	{
    		this.code = code;
    	}
	}
	/** Usb Interface to TIC */
	private UsbDevice ticDev = null;
	
	/** Model number of TIC to search for (0 for Any) */
	private int searchModelNum;
	
	/** Serial number of TIC to search for (null for Any) */
    private String searchSerialNum;
    
    /** Flag to indicate if we are currently searching for a TIC */
    private boolean isSearching = false;
    
    /** Map of the settings to apply when we find a TIC */
    private Map<TicSet,Long> ticSettings;
    
    /**
     * The interface maintains hotpluggability by running a timer task to make
     * sure there is an active tic every 500ms.  This timer manages those tasks
     * for every interface.
     */
    private static final Timer searchTimer = new Timer("TicSearchTimer", true);
    
    /**
     * This task checks to see if ticDev is null (no TIC).  If so, it looks for
     * a Tic and applies the stored settings to it.
     */
    private final TimerTask searchTask = new TimerTask() {
    	
    	public void run() {	
    		if( ticDev == null )
    		{
    			isSearching = true;
    			if ( assignTicDev() )
    			{
    				applySettings_impl();
       			}
    			isSearching = false;
    		}
        }
    	
    };
        
    /**
     * Constructor: Attach to any TIC
     */
    public TicInterface()
    {   
    	this.searchModelNum = 0;
    	this.searchSerialNum = null;
    	searchTimer.schedule(searchTask, 0, 500);
    }
   
    /** Constructor: Attach to any TIC of specified Model
     * 
     * @param model - Model of TIC to use
     */
    public TicInterface(TicModel model)
    {   
    	this.searchModelNum =  model.code;
    	this.searchSerialNum = null;
    	searchTimer.schedule(searchTask, 0, 500);
    }
    
   
    /**
     * Constructor: Attach to TIC of specific model and serial #
     * @param model - Model of TIC to use
     * @param serial - Serial number of TIC to use
     */
    public TicInterface(TicModel model, String serial)
    {   
    	this.searchModelNum =  model.code;
    	this.searchSerialNum = serial;
    	searchTimer.schedule(searchTask, 0, 500);
    }
    
    /**
     * Save a map of settings that will be applied to any TIC
     * that is assigned to the interface.
     * @param settingsIn - Map of settings and valuse to apply
     */
	public void applySettings(Map<TicSet,Long> settingsIn) {      
		this.ticSettings = new HashMap<> (settingsIn);
		
		applySettings_impl();

 	}
	
	/**
	 * Set the position of the motor 
	 * @param pos - position (in microsteps)
	 */
	public void setPosition(int pos)
	{
		try {
			TicCmd.SET_TARGET_POSITION.Send(this,pos);
		} catch (UsbDisconnectedException | UsbException e) {
			// Deliberately swallow
		}
	}
	
	/**
	 * Get the current position of the motor
	 * @return the position in microsteps (null if no TIC connected)
	 */
	public Integer getPosition()
	{
		try {
			return (int)TicVar.CURRENT_POSITION.get(this);
		} catch (UsbDisconnectedException | UsbException e) {
			return null;
		}
	}
	
	/**
	 * Get the velocity of the motor
	 * @return the velocity in microsteps per sec (null if no tic connected)
	 */
	public Integer getVelocity()
	{
		try {
			return (int)TicVar.CURRENT_VELOCITY.get(this);
		} catch (UsbDisconnectedException | UsbException e) {
			return null;
		}
	}

	/**
	 * Stop the motor and set the current position to step 0.
	 * @param value - value to set at this position
	 */
	public void setHome()
	{
		try
		{
			TicCmd.SET_TARGET_VELOCITY.Send(this,0);
			while( 0 != TicVar.CURRENT_VELOCITY.get(this) )
			{
				// Wait until it stops
			}
			// Set new position to 0
			TicCmd.HALT_AND_SET_POSITION.Send(this,0);
		}
		catch (UsbDisconnectedException | UsbException e) 
		{
			// Deliberately swallow
		}
	}

	/**
	 * Does the actual applying of settings to the TIC.
	 * called when settings are first applied and also when the 
	 * TIC is reconnected.
	 */
	private void applySettings_impl()
	{
		try 
		{
			if(ticSettings != null)
			{
				for (TicSet key : this.ticSettings.keySet())
				{
					key.set(this, this.ticSettings.get(key));
				}
			}
			TicCmd.REINITIALIZE.Send(this);
	 		TicCmd.ENERGIZE.Send(this);
		}
		catch (UsbDisconnectedException | UsbException e)
		{
			// Swallow these exceptions.  The Tic is missing and 
			// will get found when it is back.
		}
	}
	
	/**
	 * Find a TIC and assign it to this interface
	 * @return true if found. false if not found
	 */
 	private boolean assignTicDev() {
 		final int TIC_VENDOR_ID = 0x1ffb;
 		UsbHub hub;
 		UsbServices services;
 		
		try {
			services = UsbHostManager_NoFile.getUsbServices();
 			hub = services.getRootUsbHub();
		} catch (SecurityException | UsbException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		try 
		{
			this.ticDev = findDevice(hub, TIC_VENDOR_ID, this.searchModelNum, this.searchSerialNum);	
		} 
		catch (SecurityException | UsbDisconnectedException e) 
		{
			// failed to find Tic.  Don't do anything, just keep looking.
		}  
		
		if ( this.ticDev == null)
		{
			return false;
		}
		else
		{
			return true;
		}
 	}
 	

 	/**
 	 * Recursively search through usb devices for a tic meeting criteria
 	 * @param hub - Root hub of system
 	 * @param vendorId - vendor ID (Use Pololu)
 	 * @param productId - Model of Tic (0 for any)
 	 * @param serialNum - serial number of Tic (null for any)
 	 * @return
 	 */
	@SuppressWarnings("unchecked")
	static private UsbDevice findDevice(UsbHub hub, int vendorId, int productId, String serialNum)
	{
	    for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
	    {
	    	 UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
	    	
	    	if (device.isUsbHub())
	        {
	            device = findDevice((UsbHub) device, vendorId, productId, serialNum);
	            if (device != null) return device;
	        }
	        
	        if ( desc.idVendor() != vendorId )
	        {
	        	continue;	
	        }
	        	
	        if (productId == 0)
	        {
	        	boolean foundMatch = false;
	        	for ( TicModel model : TicModel.values() )
	       		{
	        		if (model.code == desc.idProduct() )
	        		{
	        			foundMatch = true;
	        			break;
	        		}	
	       		}
	        	
	        	if (!foundMatch)
	        	{
        			continue;
        		}
	        }
	        else // product Id is set
	        {
	        	if(desc.idProduct() != productId)
	        	{
	        		continue;
	        	}
	        }
	        
	        try 
	        {
				if (serialNum != null && serialNum != device.getSerialNumberString())
				{
					continue;
				}
			} catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) 
	        {
				continue;
			}
	        
	        // If we made it through all the checks, we have a matching device.
	        return device;
	        
	    }
	    return null;
	}
	
	
	/**
	 * This is effectively a callback to the TicInterface instance from the TicCmd enum.
	 * Actually issuing it here allows us to handle errors for this tic interface
	 * @param bmRequestType - USB request type
	 * @param bRequest - USB request
	 * @param wValue - USB value
	 * @param wIndex - USB index
	 * @param len - Data length
	 * @return USB response data
	 * @throws UsbDisconnectedException , UsbException - Indicates a missing USB device
	 */
	protected byte[] SyncIrp(byte bmRequestType, byte bRequest, short wValue, short wIndex, int len) throws UsbDisconnectedException , UsbException
	{
		if (this.ticDev == null || this.isSearching)
		{
			throw new UsbException("No Tic Found");
		}
		
		UsbControlIrp irp = this.ticDev.createUsbControlIrp(
				bmRequestType,
        	    bRequest,
        	    wValue,
        	    wIndex
        	    );

		if (len != 0)
		{
			irp.setLength(len);
			irp.setData(new byte[len]);
		}
		
		try {
			ticDev.syncSubmit(irp);
		} catch (IllegalArgumentException e) {
			// This is a programming error
			e.printStackTrace();
		} catch ( UsbDisconnectedException | UsbException e) {
			// This is a missing device or HW error
			this.ticDev = null;
			throw e;
		}
		
		if (len != 0)
		{
			return irp.getData();
		}
		else
		{
			return null;
		}

	
	}

}
