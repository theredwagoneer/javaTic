package com.github.theredwagoneer.javaTic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;

/**
 * Enumerates the settings in the TIC and provides a means to set 
 * set and get them.
 * @author theredwagoneer
 *
 */
public enum TicSet {
	CONTROL_MODE                   (0x01, 1   , 0xFF, SetT.UNSIGNED),
	NEVER_SLEEP                    (0x02, 1   , 0   , SetT.BOOL    ),
	DISABLE_SAFE_START             (0x03, 1   , 0   , SetT.BOOL    ),
	IGNORE_ERR_LINE_HIGH           (0x04, 1   , 0   , SetT.BOOL    ),
	AUTO_CLEAR_DRIVER_ERROR        (0x08, 1   , 0   , SetT.BOOL    ),
	SOFT_ERROR_RESPONSE            (0x53, 1   , 0xFF, SetT.UNSIGNED),
	SOFT_ERROR_POSITION            (0x54, 4   , 0xFF, SetT.SIGNED  ),
	SERIAL_BAUD_RATE               (0x05, 2   , 0xFF, SetT.UNSIGNED),
	SERIAL_DEVICE_NUMBER           (0x07, 0xFF, 0x69, SetT.BIT_14  ),
	SERIAL_ALT_DEVICE_NUMBER       (0x6A, 0xFF, 0x6B, SetT.BIT_14  ),
	SERIAL_ENABLE_ALT_DEVICE_NUMBER(0x6A, 1   , 7   , SetT.BOOL    ),
	SERIAL_14BIT_DEVICE_NUMBER     (0x0B, 1   , 3   , SetT.BOOL    ),
	COMMAND_TIMEOUT                (0x09, 2   , 0xFF, SetT.UNSIGNED),
	SERIAL_CRC_FOR_COMMANDS        (0x0B, 1   , 0   , SetT.BOOL    ),
	SERIAL_CRC_FOR_RESPONSES       (0x0B, 1   , 1   , SetT.BOOL    ),
	SERIAL_7BIT_RESPONSES          (0x0B, 1   , 2   , SetT.BOOL    ),
	SERIAL_RESPONSE_DELAY          (0x5E, 1   , 0xFF, SetT.UNSIGNED),
	VIN_CALIBRATION                (0x14, 2   , 0xFF, SetT.SIGNED  ),
	INPUT_AVERAGING_ENABLED        (0x2E, 1   , 0   , SetT.BOOL    ),
	INPUT_HYSTERESIS               (0x2F, 2   , 0xFF, SetT.UNSIGNED),
	INPUT_SCALING_DEGREE           (0x20, 1   , 0xFF, SetT.UNSIGNED),
	INPUT_INVERT                   (0x21, 1   , 0xFF, SetT.UNSIGNED),
	INPUT_MIN                      (0x22, 2   , 0xFF, SetT.UNSIGNED),
	INPUT_NEUTRAL_MIN              (0x24, 2   , 0xFF, SetT.UNSIGNED),
	INPUT_NEUTRAL_MAX              (0x26, 2   , 0xFF, SetT.UNSIGNED),
	INPUT_MAX                      (0x28, 2   , 0xFF, SetT.UNSIGNED),
	OUTPUT_MIN                     (0x2A, 4   , 0xFF, SetT.SIGNED  ),
	OUTPUT_MAX                     (0x32, 4   , 0xFF, SetT.SIGNED  ),
	ENCODER_PRESCALER              (0x58, 4   , 0xFF, SetT.UNSIGNED),
	ENCODER_POSTSCALER             (0x37, 4   , 0xFF, SetT.UNSIGNED),
	ENCODER_UNLIMITED              (0x5C, 1   , 0xFF, SetT.UNSIGNED),
	SCL_CONFIG                     (0x3B, 1   , 0xFF, SetT.UNSIGNED),
	SDA_CONFIG                     (0x3C, 1   , 0xFF, SetT.UNSIGNED),
	TX_CONFIG                      (0x3D, 1   , 0xFF, SetT.UNSIGNED),
	RX_CONFIG                      (0x3E, 1   , 0xFF, SetT.UNSIGNED),
	RC_CONFIG                      (0x3F, 1   , 0xFF, SetT.UNSIGNED),
	INVERT_MOTOR_DIRECTION         (0x1B, 1   , 0   , SetT.BOOL    ),
	MAX_SPEED                      (0x47, 4   , 0xFF, SetT.UNSIGNED),
	STARTING_SPEED                 (0x43, 4   , 0xFF, SetT.UNSIGNED),
	MAX_ACCEL                      (0x4F, 4   , 0xFF, SetT.UNSIGNED),
	MAX_DECEL                      (0x4B, 4   , 0xFF, SetT.UNSIGNED),
	STEP_MODE                      (0x41, 1   , 0xFF, SetT.UNSIGNED),
	CURRENT_LIMIT                  (0x40, 1   , 0xFF, SetT.UNSIGNED),
	CURRENT_LIMIT_DURING_ERROR     (0x31, 1   , 0xFF, SetT.UNSIGNED),
	DECAY_MODE                     (0x42, 1   , 0xFF, SetT.UNSIGNED),
	AUTO_HOMING                    (0x02, 1   , 0   , SetT.BOOL    ),
	AUTO_HOMING_FORWARD            (0x03, 1   , 2   , SetT.BOOL    ),
	HOMING_SPEED_TOWARDS           (0x61, 4   , 0xFF, SetT.UNSIGNED),
	HOMING_SPEED_AWAY              (0x65, 4   , 0xFF, SetT.UNSIGNED);
	
	/**
	 * Enumerates the different ways the settings are packed
	 */
	enum SetT{
		SIGNED,UNSIGNED,BIT_14,BOOL;
	}
	
	private final byte offset;
	private final byte len;
	private final byte aux;
	private final SetT type;
	
	/**
	 * Constructor
	 * @param offset - Offset in TIC memory
	 * @param len - Length in bytes.  This is set to 1 byte for BOOL
	 * 				and is ignored for BIT_14 packed settings
	 * @param aux - Bit index for BOOLs and secondary offset for BIT_14s.  
	 * 				It is ignored for other packing types.
	 * @param type - Packing type for the setting.
	 */
	TicSet(int offset, int len, int aux, SetT type)
	{
		this.offset = (byte)offset;
		this.len    = (byte)len;
		this.aux    = (byte)aux;
		this.type   = type;
	}
	
	/**
	 * Get the value of the setting
	 * @param tic - The Tic interface to use
	 * @return The value of the setting as a long which is big enough to handle
	 * 			unsigned ints.
	 * @throws UsbException - Missing Device
	 * @throws UsbDisconnectedException - Missing Device
	 */
	public long get(TicInterface tic) throws UsbDisconnectedException, UsbException 
	{
		byte bytes[];
		long retval=0;
		ByteBuffer buff;
		
		switch(this.type)
		{
			case SIGNED:
				bytes = TicCmd.GET_SETTING.Send(tic, this.offset, this.len);
				buff = ByteBuffer.wrap(bytes);
				buff.order(ByteOrder.LITTLE_ENDIAN);
				
				switch(this.len)
				{
					case 1:
						retval = (long) bytes[0];
						break;
					case 2:
						retval = (long) buff.getShort();
						break;
					case 4:
						retval = (long) buff.getInt();
						break;
					default:
						assert(false);
						break;
				}
				break;
				
			case UNSIGNED:
			case BOOL:
				bytes = TicCmd.GET_SETTING.Send(tic, this.offset, this.len);
				byte paddedBytes[] = new byte[8];
				
				for( int i = 0; i<len; ++i)
				{
					paddedBytes[i] = bytes[i];
				}
				
				buff = ByteBuffer.wrap(paddedBytes);
				buff.order(ByteOrder.LITTLE_ENDIAN);
				retval = buff.getLong();
				
				if ( this.type == SetT.BOOL )
				{
					retval = ( retval >> aux ) & 1; 
				}
				
				break;
				
			case BIT_14:
				byte lower[] = TicCmd.GET_SETTING.Send(tic, this.offset, (byte) 1);
				byte upper[] = TicCmd.GET_SETTING.Send(tic, this.aux,    (byte) 1);
				
				retval = (upper[0] & 0x7F);
				
				retval <<= 7;
				retval |= (lower[0] & 0x7F);
				
				break;
				
			default:
				assert(false);
				break;		
		}
		
		return retval;
		
	}
	/**
	 * Sets the parameter
	 * Note: These are save to EEPROM which has like 100,000 writes, 
	 * so don't call this rapidly
	 * @param tic - The tic interface
	 * @param dataToSet - The data to send to the tic
	 * @throws UsbException - Missing Device
	 * @throws UsbDisconnectedException - Missing Device
	 */
	public void set(TicInterface tic, long dataToSet) throws UsbDisconnectedException, UsbException 
	{
		byte bytesToSet[] = new byte[8];
		byte currByte[];
		ByteBuffer buffToSet = ByteBuffer.wrap(bytesToSet);
		buffToSet.order(ByteOrder.LITTLE_ENDIAN);
		
		switch(this.type)
		{
			case SIGNED:
			case UNSIGNED:
				buffToSet.putLong(dataToSet);
				break;
				
			case BOOL:
				assert (this.aux < 8 && this.len == 1); // Needs to be in the first byte
				assert (dataToSet == 1 || dataToSet == 0); // Only acceptable bools
				
				bytesToSet = TicCmd.GET_SETTING.Send(tic, this.offset, this.len);
				
				if (dataToSet == 1)
				{
					bytesToSet[0] |= (1 << aux);
				}
				else
				{
					bytesToSet[0] &= ~(1 << aux);
				}
				break;
				
			case BIT_14:
				byte lower[] = TicCmd.GET_SETTING.Send(tic, this.offset, (byte) 1);
				byte upper[] = TicCmd.GET_SETTING.Send(tic, this.aux,    (byte) 1);
				
				lower[0] &= ~0x7F;
				upper[0] &= ~0x7F;
				
				lower[0] |= (byte) (dataToSet & 0x7F);
				upper[0] |= (byte)((dataToSet >> 7) & 0x7F);
				
				bytesToSet[0] = lower[0];
				bytesToSet[1] = upper[0]; 
				
				break;
				
			default:
				assert(false);
				break;		
		}
		
		if (this.type == SetT.BIT_14) 
		{
			// Handle special case
			
			currByte = TicCmd.GET_SETTING.Send(tic, this.offset, (byte) 1);
			if (currByte[0] != bytesToSet[0])
			{
				TicCmd.SET_SETTING.Send(tic, this.offset, (short) bytesToSet[0]);
			}
			
			currByte = TicCmd.GET_SETTING.Send(tic, this.aux, (byte) 1);
			if (currByte[0] != bytesToSet[1])
			{
				TicCmd.SET_SETTING.Send(tic, this.aux, (short) bytesToSet[1]);
			}
		}
		else
		{
			for(short i = 0, offCnt = this.offset; i < this.len; i++, offCnt++)
			{
				currByte = TicCmd.GET_SETTING.Send(tic, offCnt, (byte) 1);
				if (currByte[0] != bytesToSet[i])
				{
					TicCmd.SET_SETTING.Send(tic, offCnt, (short) bytesToSet[i]);
				}
			}
		}
		
	}
}
