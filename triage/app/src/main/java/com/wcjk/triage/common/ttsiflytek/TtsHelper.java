package com.wcjk.triage.common.ttsiflytek;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.wcjk.triage.event.ShowCallEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class TtsHelper {
	private com.wcjk.triage.common.utils.Log log = com.wcjk.triage.common.utils.Log.getLogger(this.getClass());

	private static String TAG = TtsHelper.class.getSimpleName();
	private static TtsHelper ttsHelper;
	private static final long INIT_RETRY_DELAY_MILLIS = 3000L;
	private static final int MAX_INIT_RETRY_COUNT = 12;
	// 语音合成对象
	private SpeechSynthesizer mTts;
	private Handler mainHandler = new Handler(Looper.getMainLooper());
	private boolean initRetryScheduled = false;
	private int initRetryCount = 0;

	// 默认云端发音人
	public String voicerCloud="xiaoyan";

	// 默认本地发音人
	public String voicerLocal="xiaoyan";
	
	//缓冲进度
	private int mPercentForBuffering = 0;	
	//播放进度
	private int mPercentForPlaying = 0;

	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_LOCAL;//SpeechConstant.TYPE_CLOUD;

	private volatile boolean isOver = true;
	private  List<CallBean> list_callBean;

	private final Object lock = new Object();
	private Thread speakThread;

	public static TtsHelper getInstance() {
		if (ttsHelper == null) {
			ttsHelper = new TtsHelper();
		}
		return ttsHelper;
	}
	/**
	 * 参数设置
	 */
	public synchronized void init(Context context){
		Context appContext = context == null ? null : context.getApplicationContext();
		if (appContext == null) {
			appContext = context;
		}
		if (appContext == null) {
			log.e("TTS 初始化失败：context 为空");
			return;
		}
		if (list_callBean == null) {
			list_callBean = new ArrayList<>();
		}
		if (mTts != null) {
			isOver = false;
			ensureSpeakThread();
			return;
		}

		try {
			StringBuffer param = new StringBuffer();
			param.append("appid="+"5cbf3a01");
			param.append(",");
			// 设置使用v5+
			param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
			SpeechUtility.createUtility(appContext, param.toString());

			// 初始化合成对象
			mTts = SpeechSynthesizer.createSynthesizer(appContext, mTtsInitListener);
			if (mTts == null) {
				log.e("TTS 初始化失败：SpeechSynthesizer 为空，等待重试");
				scheduleInitRetry(appContext);
				return;
			}

			configureTts(appContext);
			initRetryCount = 0;
			initRetryScheduled = false;
			isOver = false;
			ensureSpeakThread();
		} catch (Exception e) {
			log.e("TTS 初始化异常：" + e.toString());
			log.e(e);
			mTts = null;
			scheduleInitRetry(appContext);
		}
	}

	public void setVolumn(boolean is){
		if (is){
			if (mTts != null) {
				mTts.setParameter(SpeechConstant.VOLUME, "100");
			}
		}else{
			if (mTts != null) {
				mTts.setParameter(SpeechConstant.VOLUME, "0");
			}
		}
	}
	public void ttsSpeak(String callText,String showText) {
		if (TextUtils.isEmpty(callText)) return;
		setCallBean(callText,showText);
	}

	private void configureTts(Context context) {
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		//设置合成
		if(mEngineType.equals(SpeechConstant.TYPE_CLOUD))
		{
			//设置使用云端引擎
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			//设置发音人
			mTts.setParameter(SpeechConstant.VOICE_NAME,voicerCloud);
		}else {
			//设置使用本地引擎
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			//设置发音人资源路径
			mTts.setParameter(ResourceUtil.TTS_RES_PATH,getResourcePath(context));
			//设置发音人
			mTts.setParameter(SpeechConstant.VOICE_NAME,voicerLocal);
		}
		//mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY,"1");//支持实时音频流抛出，仅在synthesizeToUri条件下支持
		//设置合成语速
		mTts.setParameter(SpeechConstant.SPEED,  "25");
		//设置合成音调
		mTts.setParameter(SpeechConstant.PITCH,  "50");
		//设置合成音量=mTts.setParameter(SpeechConstant.VOLUME,  "100");
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "0");

		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
		mTts.setParameter("rdn", "2"); //2数字读成阿拉伯数字
	}

	private void scheduleInitRetry(final Context context) {
		if (initRetryScheduled) {
			return;
		}
		if (initRetryCount >= MAX_INIT_RETRY_COUNT) {
			log.e("TTS 初始化失败：已达到最大重试次数");
			return;
		}
		initRetryCount++;
		initRetryScheduled = true;
		log.w("TTS 初始化将在 " + INIT_RETRY_DELAY_MILLIS + "ms 后重试，第 " + initRetryCount + " 次");
		mainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				synchronized (TtsHelper.this) {
					initRetryScheduled = false;
				}
				init(context);
			}
		}, INIT_RETRY_DELAY_MILLIS);
	}

	private void ensureSpeakThread() {
		if (speakThread != null && speakThread.isAlive()) {
			return;
		}
		speakThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!isOver) {
					try {
						Thread.sleep(500);
						SpeechSynthesizer tts = mTts;
						if (tts == null) {
							continue;
						}
						if (!tts.isSpeaking()) {
							CallBean callBean = getCallBean();
							if (callBean != null) {
								String callText = callBean.getCallText();
								String showText = callBean.getShowText();
								if (!TextUtils.isEmpty(showText)) {
									if (EventBus.getDefault().hasSubscriberForEvent(ShowCallEvent.class)) {
										EventBus.getDefault().post(new ShowCallEvent(showText));
									}
									speak(callText);
								}
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					} catch (Exception e) {
						log.e("TTS 播报线程异常：" + e.toString());
						log.e(e);
					}
				}
			}
		}, "TtsSpeakThread");
		speakThread.start();
	}

	public void speak(String text) {
		if (TextUtils.isEmpty(text)) return;
		SpeechSynthesizer tts = mTts;
		if (tts == null) {
			log.e("语音合成未初始化，跳过播报：" + text);
			return;
		}
		int code = tts.startSpeaking(text, mTtsListener);
//			/**
//			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
//			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
//			*/
//			String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
//			int code = mTts.synthesizeToUri(text, path, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			log.e("语音合成失败,错误码: " + code);
		}
	}

	private CallBean getCallBean(){
		synchronized (lock) {
			if (list_callBean != null && list_callBean.size() > 0) {
				CallBean callBean = list_callBean.get(0);
				if (callBean != null) {
					String callText = callBean.getCallText();
					String showText = callBean.getShowText();
					log.d("当前语音播报队列长度:" + list_callBean.size() + ",播报内容:" + callText);
				}

				list_callBean.remove(0);
				return callBean;
			}

			return null;
		}
	}

	private void setCallBean(String callText, String showText){
		synchronized (lock) {
			if (TextUtils.isEmpty(callText)) return;
			if (list_callBean == null) {
				list_callBean = new ArrayList<>();
			}

			if (list_callBean != null) {
				while (list_callBean.size() >= 10) {
					log.d("队列过长，删除最早播报内容:" + list_callBean.get(0));
					list_callBean.remove(0);
				}
				log.d("当前语音播报队列长度:" + list_callBean.size() + ",添加播报内容:" + callText);
				list_callBean.add(new CallBean(callText, showText));
			}

		}
	}

	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			log.d("InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
//        		showTip("初始化失败,错误码："+code);
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}		
		}
	};

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		
		@Override
		public void onSpeakBegin() {
			log.d("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			log.d("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			log.d("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			mPercentForBuffering = percent;
//			showTip(String.format(getString(R.string.tts_toast_format),
//					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
//			showTip(String.format(getString(R.string.tts_toast_format),
//					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				log.d("播放完成");
			} else if (error != null) {
				log.d(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
			
			//实时音频流输出参考
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
				byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
				Log.e("MscSpeechLog", "buf is =" + buf);
			}*/
		}
	};

	//获取发音人资源路径
	private String getResourcePath(Context context){
		StringBuffer tempBuffer = new StringBuffer();
		//合成通用资源
		tempBuffer.append(ResourceUtil.generateResourcePath(context, RESOURCE_TYPE.assets, "tts/common.jet"));
		tempBuffer.append(";");
		//发音人资源
		tempBuffer.append(ResourceUtil.generateResourcePath(context, RESOURCE_TYPE.assets, "tts/"+voicerLocal+".jet"));
		return tempBuffer.toString();
	}
	

	public synchronized void destroy() {
		isOver = true;
		if (speakThread != null) {
			speakThread.interrupt();
			speakThread = null;
		}
		if( null != mTts ){
			mTts.stopSpeaking();
			// 退出时释放连接
			mTts.destroy();
		}
		mTts = null;
	}
}
