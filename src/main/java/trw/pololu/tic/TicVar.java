package trw.pololu.tic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;


/**
 * Enumerates the status variables in the TIC and provides a
 * mechanism to retrieve their values.
 * @author theredwagoneer
 *
 */
public enum TicVar {
	OPERATION_STATE         (0x00, 1, false),
	MISC_FLAGS_1            (0x01, 1, false),
	ERROR_STATUS            (0x02, 2, false),
	ERRORS_OCCURRED         (0x04, 4, false),
	PLANNING_MODE           (0x09, 1, false),
	TARGET_POSITION         (0x0A, 4, true ),
	TARGET_VELOCITY         (0x0E, 4, true ),
	STARTING_SPEED          (0x12, 4, false),
	MAX_SPEED               (0x16, 4, false),
	MAX_DECELERATION        (0x1A, 4, false),
	MAX_ACCELERATION        (0x1E, 4, false),
	CURRENT_POSITION        (0x22, 4, true ),
	CURRENT_VELOCITY        (0x26, 4, true ),
	ACTING_TARGET_POSITION  (0x2A, 4, true ),
	TIME_SINCE_LAST_STEP    (0x2E, 4, false),
	DEVICE_RESET            (0x32, 1, false),
	VIN_VOLTAGE             (0x33, 2, false),
	UP_TIME                 (0x35, 4, false),
	ENCODER_POSITION        (0x39, 4, true ),
	RC_PULSE_WIDTH          (0x3D, 2, false),
	ANALOG_READING_SCL      (0x3F, 2, false),
	ANALOG_READING_SDA      (0x41, 2, false),
	ANALOG_READING_TX       (0x43, 2, false),
	ANALOG_READING_RX       (0x45, 2, false),
	DIGITAL_READINGS        (0x47, 1, false),
	PIN_STATES              (0x48, 1, false),
	STEP_MODE               (0x49, 1, false),
	CURRENT_LIMIT           (0x4A, 1, false),
	DECAY_MODE              (0x4B, 1, false),
	INPUT_STATE             (0x4C, 1, false),
	INPUT_AFTER_AVERAGING   (0x4D, 2, false),
	INPUT_AFTER_HYSTERESIS  (0x4F, 2, false),
	INPUT_AFTER_SCALING     (0x51, 4, true ),
	LAST_MOTOR_DRIVER_ERROR (0x55, 1, false),
	AGC_MODE                (0x56, 1, false),
	AGC_BOTTOM_CURRENT_LIMIT(0x57, 1, false),
	AGC_CURRENT_BOOST_STEPS (0x58, 1, false),
	AGC_FREQUENCY_LIMIT     (0x59, 1, false),
	LAST_HP_DRIVER_ERRORS   (0xFF, 1, false);
	
	
	private final byte code;
	private final byte len;
	private final boolean signed;
	
	/**
	 * Constructor
	 * @param code - The Number of the variable in the TIC
	 * @param len - The byte length of the variable
	 * @param signed - true if the variable is signed, false if not
	 */
	TicVar (int code, int len, boolean signed)
	{
		this.code = (byte)code;
		this.len = (byte)len;
		this.signed = signed;
	}
	
	/**
	 * Gets the value of a variable and puts it in a long.  Long is
	 * used for everything because it is guaranteed to hold unsigned
	 * 32 bit numbers
	 * @param  tic - The tic interface to use
	 * @return long representation of the value of the variable. 
	 * @throws UsbException - Missing Device
	 * @throws UsbDisconnectedException - Missing DEvice
	 */
	public long get(TicInterface tic) throws UsbDisconnectedException, UsbException 
	{
		byte bytes[] = TicCmd.GET_VARIABLE.Send(tic, this.code, this.len);
		
		if (this.signed == true)
		{
			assert(this.len == 4); // 4 bytes id the only signed length supported as of 7/6/2021
			ByteBuffer buff = ByteBuffer.wrap(bytes);
			buff.order(ByteOrder.LITTLE_ENDIAN);
			
			return (long) buff.getInt();
		}
		else
		{
			byte paddedBytes[] = new byte[8];
			for( int i = 0; i<len; ++i)
			{
				paddedBytes[i] = bytes[i];
			}
			
			ByteBuffer buff = ByteBuffer.wrap(paddedBytes);
			buff.order(ByteOrder.LITTLE_ENDIAN);
			
			return (long) buff.getInt();
		}
		
	}
}
