package com.app.common.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * OrientationSensorUtils 方向传感器工具类
 */
public final class OrientationSensorUtils {

	private Context context;
	private float[] accelerometerValues,magneticValues;
	
	private SensorManager mSensorManager;
	
	private SMListener magneticListener;
	private SMListener accelerometerListener;
	
	private SensorEventListener mOrientationSensorEventListener;

	private Double lastAzimuth;
	private Double lastPitch;
	private Double lastYaw;
	private Double lastRoll;

	private boolean starting = false;

	private double minOffset = 1;

	public void setOnOrientationListencer(OnOrientationListencer onOrientationListencer) {
		this.mOnOrientationListencer = onOrientationListencer;
	}

	private OnOrientationListencer mOnOrientationListencer;
	
	private boolean useNewInterface = false;//是否使用官方新接口,新接口获取的方向总是跳动,所以还是用弃用的接口
	
	private OrientationSensorUtils(Context context, boolean useNewInterface) {
		this.context = context;
		this.useNewInterface = useNewInterface;
		this.mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	public static OrientationSensorUtils getInstance(Context context){
		return new OrientationSensorUtils(context,false);
	}
	
	public static OrientationSensorUtils getInstance(Context context,boolean useNewInterface){
		return new OrientationSensorUtils(context,useNewInterface);
	}
	
	public synchronized boolean startOrientationSensor(){
		if(starting){
			return starting;
		}
		starting = true;
		if(useNewInterface){
			return registerNewListener();
		}else{
			if(registerOldListener()){
				return true;
			}else{
				return registerNewListener();
			}
		}
	}

	private boolean registerNewListener(){
		if(accelerometerListener != null && magneticListener != null){
			return true;
		}
		Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if(accel == null || magnetic == null){
			return false;
		}
		accelerometerListener = new SMListener();
		magneticListener = new SMListener();
		mSensorManager.registerListener(accelerometerListener, accel, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(magneticListener, magnetic, SensorManager.SENSOR_DELAY_UI);
		minOffset = 1.3;
		return true;
	}

	private boolean registerOldListener(){
		if(mOrientationSensorEventListener != null){
			return true;
		}
		Sensor orientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		if(orientationSensor == null){
			return false;
		}
		mOrientationSensorEventListener = new SMListener();
		mSensorManager.registerListener(mOrientationSensorEventListener,orientationSensor, SensorManager.SENSOR_DELAY_UI);
		return true;
	}
	
	public void stopOrientationSensor(){
		if(mOnOrientationListencer != null){
			this.mOnOrientationListencer = null;
		}
		if(useNewInterface){
			if(magneticListener == null && accelerometerListener == null){
				return;
			}
			mSensorManager.unregisterListener(magneticListener);
			mSensorManager.unregisterListener(accelerometerListener);
		}else{
			if(mOrientationSensorEventListener == null){
				return;
			}
			mSensorManager.unregisterListener(mOrientationSensorEventListener);
		}
		mOrientationSensorEventListener = accelerometerListener = magneticListener = null;
		starting = false;
	}

	private void updateOrientation(){
		getOrientation();
		if(lastAzimuth != null){
			if(mOnOrientationListencer != null){
//					if(Math.abs(azimuth-lastAzimuth) >= minOffset){
//						lastAzimuth = azimuth;
					mOnOrientationListencer.onChange(this);
//					}
			}
		}
	}

	public Double getLastAzimuth(){
		return lastAzimuth!=null?lastAzimuth:0;
	}
	public Double getLastPitch(){
		return lastPitch!=null?lastPitch:0;
	}

	public Double getLastYaw(){
		return lastYaw!=null?lastYaw:0;
	}
	public Double getLastRoll(){
		return lastRoll!=null?lastRoll:0;
	}


	public String getLastAzimuthStr(){
		int fwj = (int) getLastAzimuth().intValue();
		String degreeStr = "";
		if (fwj >= 355){
			degreeStr+="正北";
		}else if(fwj < 5) {
			degreeStr+="正北";
		} else if (fwj >= 5 && fwj < 85) {
			// Log.i(TAG, "东北");
			degreeStr+="东北"+fwj+"°";
		} else if (fwj >= 85 && fwj < 95) {
			// Log.i(TAG, "正东");
			degreeStr+="正东";
		} else if (fwj >= 95 && fwj < 175) {
			// Log.i(TAG, "东南");
			fwj = 180-fwj;
			degreeStr+="东南"+fwj+"°";
		} else if ((fwj >= 175 && fwj < 185)) {
			// Log.i(TAG, "正南");
			degreeStr+="正南";
		} else if (fwj >= 185 && fwj < 265) {
			// Log.i(TAG, "西南");
			fwj = 270-fwj;
			degreeStr+="西南"+fwj+"°";
		} else if (fwj >= 265 && fwj < 275) {
			// Log.i(TAG, "正西");
			degreeStr+="正西";
		} else if (fwj >= 275 && fwj < 355) {
			// Log.i(TAG, "西北");
			fwj = 360-fwj;
			degreeStr+="西北"+fwj+"°";
		}
		return degreeStr;
	}
	
	private void getOrientation(){
		//获取设备自然方向
		/*
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		int rotation = display.getRotation();
		
		int x_axis = SensorManager.AXIS_X;
		int y_axis = SensorManager.AXIS_Y;
		switch (rotation) {
		case Surface.ROTATION_0:
			break;
		case Surface.ROTATION_90:
			x_axis = SensorManager.AXIS_Y;
			y_axis = SensorManager.AXIS_MINUS_X;
			break;
		case Surface.ROTATION_270:
			x_axis = SensorManager.AXIS_MINUS_Y;
			y_axis = SensorManager.AXIS_X;
			break;
		case Surface.ROTATION_180:
			y_axis = SensorManager.AXIS_MINUS_Y;
			break;

		default:
			break;
		}
		*/
		
		//获取方向
		float values[] = null;
		if(useNewInterface && magneticValues != null && accelerometerValues != null){
			float[] inR = new float[9];
			boolean check = SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticValues);
			if(check){
				/**重定义坐标系*/
//				float[] outR = new float[9];
//				SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);
//				SensorManager.getOrientation(outR, values);
				values = new float[3];
				SensorManager.getOrientation(inR, values);
				lastAzimuth = ((360f+Math.toDegrees(values[0]))%360);  //方位角;
				lastPitch =  Math.toDegrees(values[0]);//俯仰角
				lastYaw =  Math.toDegrees(values[1]);//偏航角
				lastRoll =  Math.toDegrees(values[2]);//横滚角
//				values[1] = (float) Math.toDegrees(values[1]);//俯仰角,绕x轴角度
//				values[2] = (float) Math.toDegrees(values[2]);//橫滚角,绕y轴角度
			}			
		}else{
//			if(mTargetDirection != null){
//				values = new float[3];
//				values[0] = mTargetDirection;
//			}
		}
//		return values;
	}
	
	class SMListener implements SensorEventListener{
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			int type = event.sensor.getType();
			switch (type) {
			case Sensor.TYPE_ACCELEROMETER:
				accelerometerValues = event.values.clone();
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				magneticValues = event.values.clone();
				break;
			case Sensor.TYPE_ORIENTATION:
				lastAzimuth = (double) event.values[SensorManager.DATA_X];// 赋值给全局变量，让指南针旋转
				if(lastAzimuth < 0){
					lastAzimuth += 360;
				}
//				KKLog.d("设备方向角度 "+lastAzimuth);
				lastPitch = (double)event.values[SensorManager.DATA_X];// 赋值给全局变量，让指南针旋转
				lastYaw = (double)event.values[SensorManager.DATA_Y];// 赋值给全局变量，让指南针旋转
				lastRoll = (double)event.values[SensorManager.DATA_Z];// 赋值给全局变量，让指南针旋转
//				mTargetDirection = event.values[SensorManager.DATA_X];// 赋值给全局变量，让指南针旋转
				break;
			default:
				break;
			}
			updateOrientation();
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	}
	
	// 调整方向传感器获取的值
//	private float normalizeDegree(float degree) {
//		return (degree + 720) % 360;
//	}

	public interface OnOrientationListencer{
		void onChange(OrientationSensorUtils sensorUtils);
	}

}

