package trw.pololu.tic;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;

/**
 * This is an enum of all the available commands for the Tic.
 * Calling the Send method on these will actually send the command.
 * You will need to provide the appropriate parameters.
 * 
 * @author theredwagoneer
 *
 */

public enum TicCmd {
	SET_TARGET_POSITION   ((byte)0xE0,CmdT.BIT_32),
	SET_TARGET_VELOCITY   ((byte)0xE3,CmdT.BIT_32),
	HALT_AND_SET_POSITION ((byte)0xEC,CmdT.BIT_32),
	HALT_AND_HOLD		  ((byte)0x89,CmdT.QUICK),
	GO_HOME				  ((byte)0x97,CmdT.BIT_7),
	RESET_COMMAND_TIMEOUT ((byte)0x8C,CmdT.QUICK),
	DEENERGIZE		      ((byte)0x86,CmdT.QUICK),
	ENERGIZE			  ((byte)0x85,CmdT.QUICK),
	EXIT_SAFE_START		  ((byte)0x83,CmdT.QUICK),
	ENTER_SAFE_START      ((byte)0x8F,CmdT.QUICK),
	RESET				  ((byte)0xB0,CmdT.QUICK),
	CLEAR_DRIVER_ERROR    ((byte)0xE6,CmdT.BIT_32),
	SET_STARTING_SPEED    ((byte)0xE5,CmdT.BIT_32),
	SET_MAX_ACCEL		  ((byte)0xEA,CmdT.BIT_32),
	SET_MAX_DECELERATION  ((byte)0xE9,CmdT.BIT_32),
	SET_STEP_MODE		  ((byte)0x94,CmdT.BIT_7),
	SET_CURRENT_LIMIT     ((byte)0x91,CmdT.BIT_7),
	SET_DECAY_MODE		  ((byte)0x92,CmdT.BIT_7),
	SET_AGC_OPTION	      ((byte)0x98,CmdT.BIT_7),
	GET_VARIABLE		  ((byte)0xA1,CmdT.BLOCK_RD),
	GET_VARIABLE_AND_CLEAR((byte)0xA2,CmdT.BLOCK_RD),
	GET_SETTING			  ((byte)0xA8,CmdT.BLOCK_RD),
	SET_SETTING			  ((byte)0x13,CmdT.SET_SETTING),
	REINITIALIZE		  ((byte)0x10,CmdT.QUICK),
	START_BOOTLOADER	  ((byte)0xFF,CmdT.QUICK);
	
	/**
	 * Captures the five formats of commands that the
	 * tic can receive. 
	 */
	static private enum CmdT {
		QUICK, BIT_7, BIT_32, BLOCK_RD,SET_SETTING;
	}
	
	private final byte code;
	private final CmdT type;
	
	/**
	 * Constructor
	 * @param code - The 16 bit code representing this command
	 * @param type - The type of command this is.
	 */
	TicCmd (byte code,CmdT type)
	{
		this.code = code;
		this.type = type;
	}
	
	/**
	 * Sends command to the for QUICK commands that
	 * don't have parameters.
	 * @param tic - The tic interface to send to.
	 * @throws UsbException - Missing device
	 * @throws UsbDisconnectedException - Missing Device
	 */
	public void Send(TicInterface tic) throws UsbDisconnectedException, UsbException
	{
		assert (CmdT.QUICK == this.type);
		
		tic.SyncIrp(
		        	(byte) 0x40,
		        	this.code,
		        	(short) 0,
		        	(short) 0,
		        	0
		        	);

	}
	
	/**
	 * Sends commands to the tic that require one value of additional
	 * data.
	 * @param tic - The tic interface to send to.
	 * @param data - The data to include with the command.
	 * @throws UsbException 
	 * @throws UsbDisconnectedException 
	 */
	public void Send(TicInterface tic, int data) throws UsbDisconnectedException, UsbException
	{
		switch(this.type)
		{
			case BIT_7:
				assert (data >= 0 && data < 128);
				tic.SyncIrp(
		        	    (byte) 0x40,
		        	    this.code,
		        	    (short) data,
		        	    (short) 0,
		        	    0
		        	    );
		        break;
			case BIT_32:
				tic.SyncIrp(
		        	    (byte) 0x40,
		        	    this.code,
		        	    (short) (data & 0xFFFF),
		        	    (short) ((data >> 16) & 0xFFFF),
		        	    0);
				break;
			default:
				assert(false);
				break;
		}
		
	}
	
	/**
	 * Sends a command to the tic that requires an offset plus additional data
	 * @param tic - The Tic interface to use
	 * @param offset - The offest to write/read from
	 * @param lenOrData - Length to read (BLOCK_RD) or Data to set (SET_SETTING)
	 * @return The requested data for a BLOCK_RD cmd or null for the SET_SETTING cmd
	 * @throws UsbException - Missing Device
	 * @throws UsbDisconnectedException - Missing Device
	 */
	public byte[] Send(TicInterface tic, short offset, short lenOrData) throws UsbDisconnectedException, UsbException 
	{
		switch(this.type)
		{
	
			case BLOCK_RD:
				byte resp[] = tic.SyncIrp(
						(byte) 0xC0,
		        	    this.code,
		        	    (short) 0,
		        	    (short) offset,
		        	    lenOrData
		        	    );
		        			
				return resp;
			case SET_SETTING:
				System.out.printf(">>>>> Setting Offset 0x%x to % d\n", offset, lenOrData);
				tic.SyncIrp(
						(byte) 0x40,
						this.code,
						(short) lenOrData,
						offset,
						0
	        	    	);
				return null;
		default:
				assert(false);
				break;
		}
		return null;
	}
	

}
