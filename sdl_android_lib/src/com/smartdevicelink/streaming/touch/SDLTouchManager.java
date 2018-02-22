package fomobile.com.smfesri.sdl.newSDL;

import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.TouchCoord;
import com.smartdevicelink.proxy.rpc.TouchEvent;
import com.smartdevicelink.proxy.rpc.enums.TouchType;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Burak on 19.02.2018.
 */

public class SDLTouchManager {

    SDLTouchManagerDelegate mCallback;

    interface SDLTouchManagerDelegate {
         void didReceiveSingleTap(@Nullable View view, TouchCoord point);
         
         void didReceiveDoubleTap(@Nullable View view, TouchCoord point);
         
         void didReceivePanning(@Nullable View view, TouchCoord fromPoint, TouchCoord toPoint);
         
         void didReceivePinch(@Nullable View view, TouchCoord atPoint, double withScale);

         void panningDidStart(@Nullable View view, TouchCoord atPoint);
         
         /**
          * @brief bla bla bla bla
          * @param view The view found by the Hit tester. @note in the current version of the sdl library the hit tester is not implemented therefore the view will always be null.
          */
         void panningDidEnd(@Nullable View view, TouchCoord atPoint);
         
         void panningCanceled(@Nullable View view, TouchCoord atPoint);
         
         void pinchDidStartInView(TouchCoord atCenterPoint);
         void pinchDidEndInView(TouchCoord atCenterPoint);
         void pinchCanceledAtCenterPoint(TouchCoord atCenterPoint);
    }

    void setSDLTouchManagerDelegate(SDLTouchManagerDelegate callback){
        this.mCallback = callback;
    }

    private enum SDLPerformingTouchType {
        SDLPerformingTouchTypeNone,
        SDLPerformingTouchTypeSingleTouch,
        SDLPerformingTouchTypeMultiTouch,
        SDLPerformingTouchTypePanningTouch;
    }

    private SDLPerformingTouchType performingTouchType;
    private TouchCoord previousTouch;
    private TouchEvent previousTouchEvent;
    private TouchCoord lastStoredTouchLocation;
    private TouchCoord lastNotifiedTouchLocation;
    private SDLPinchGesture currentPinchGesture;
    private double previousPinchDistance;
    private Timer singleTapTimer;
    private TouchEvent singleTapTouchEvent;

    //Constants (Please see the Constructor for the values)
    private int maximumNumberOfTouches;
    private long movementTimeThreshold; //millisecond
    private long tapTimeThreshold; //milliseconds
    private long tapDistanceThreshold;
    private boolean syncedPanningEnabled;
    private boolean touchEnabled;


    public SDLTouchManager(){

        movementTimeThreshold = 50;
        tapTimeThreshold = 400;
        tapDistanceThreshold = 50;
        touchEnabled = true;
        syncedPanningEnabled = true;
        maximumNumberOfTouches = 2;

    }

    private void syncFrame(){

    }

    public void sdl_onTouchEvent (OnTouchEvent notification){
        TouchEvent touchEvent  = notification.getEvent().get(0);
        TouchType touchType = notification.getType();
        if ((!touchEnabled) || touchEvent.getId()>maximumNumberOfTouches){
            return;
        }

        switch (touchType){
            case BEGIN:
                sdl_handleOnTouchBegan(touchEvent);
                break;

            case MOVE:
                sdl_handleTouchMoved(touchEvent);
                break;

            case END:
                sdl_handleTouchEnded(touchEvent);
                break;

            case CANCEL:
                sdl_handleTouchCanceled(touchEvent);
                break;
        }

    }

    private void sdl_handleOnTouchBegan(TouchEvent touchEvent){

        TouchCoord touch = touchEvent.getTouchCoordinates().get(0);
        performingTouchType = SDLPerformingTouchType.SDLPerformingTouchTypeSingleTouch;
        previousTouchEvent = touchEvent;

        switch (touchEvent.getId()){
            case 0: //First Finger
                previousTouch = touch;
                break;

            case 1: //Second Finger
                performingTouchType = SDLPerformingTouchType.SDLPerformingTouchTypeMultiTouch;
                currentPinchGesture = new SDLPinchGesture(previousTouch,touch);
                previousPinchDistance = currentPinchGesture.getDistance();
                //TODO: Add UI Hit tester here
                mCallback.pinchDidStartInView(currentPinchGesture.getCenter());
                break;
        }

    }

    private void sdl_handleTouchMoved(TouchEvent touchEvent){

        if ((touchEvent.getTimestamps().get(0)-previousTouchEvent.getTimestamps().get(0) > movementTimeThreshold) ||
                !syncedPanningEnabled){
            return;
        }

        TouchCoord currentTouchCoord = touchEvent.getTouchCoordinates().get(0);

        switch (performingTouchType){
            case SDLPerformingTouchTypeMultiTouch:
                switch (touchEvent.getId()){
                    case 0: //First Finger
                        currentPinchGesture.setFirstTouch(currentTouchCoord);
                        break;
                    case 1: //Second Finger
                        currentPinchGesture.setSecondTouch(currentTouchCoord);
                        break;
                }
                if (!syncedPanningEnabled){
                    syncFrame();
                }
                break;

            case SDLPerformingTouchTypeSingleTouch:
                lastNotifiedTouchLocation = currentTouchCoord;
                lastStoredTouchLocation = currentTouchCoord;

                performingTouchType=SDLPerformingTouchType.SDLPerformingTouchTypePanningTouch;
                //TODO: Add UI Hit tester here
                mCallback.panningDidStartInView(currentPinchGesture.getCenter());
                break;

            case SDLPerformingTouchTypePanningTouch:
                if (!syncedPanningEnabled){
                    syncFrame();
                }
                lastStoredTouchLocation = currentTouchCoord;
                break;

            case SDLPerformingTouchTypeNone:
                break;
        }
        previousTouch = currentTouchCoord;
        previousTouchEvent = touchEvent;

    }

    private void sdl_handleTouchEnded(TouchEvent touchEvent){

        TouchCoord currentTouchCoord = touchEvent.getTouchCoordinates().get(0);

        switch (performingTouchType){
            case SDLPerformingTouchTypeMultiTouch:
                sdl_setMultiTouchFingerTouchForTouch(touchEvent);
                if (currentPinchGesture.isValid()) {
                    //TODO: Add UI Hit tester here
                    mCallback.pinchDidEndInView(currentPinchGesture.getCenter());
                }
                currentPinchGesture = null;
                break;

            case SDLPerformingTouchTypePanningTouch:
                //TODO: Add UI Hit tester here
                mCallback.panningDidEndInView(currentTouchCoord);
                break;

            case SDLPerformingTouchTypeSingleTouch:
                if (singleTapTimer==null){
                    sdl_initializeSingleTapTimerAtPoint(currentTouchCoord);
                    singleTapTouchEvent = touchEvent;
                }else{
                    sdl_cancelSingleTapTimer();

                    TouchCoord firstTap = singleTapTouchEvent.getTouchCoordinates().get(0);
                    TouchCoord secondTap = touchEvent.getTouchCoordinates().get(0);

                    long deltaTimeStamp = touchEvent.getTimestamps().get(0).longValue() - singleTapTouchEvent.getTimestamps().get(0).longValue();
                    int deltaX = firstTap.getX() - secondTap.getX();
                    int deltaY = firstTap.getY() - secondTap.getY();

                    if ((deltaTimeStamp<= tapTimeThreshold) && (deltaX<=tapDistanceThreshold) && (deltaY<=tapDistanceThreshold)){
                        //TODO: Add UI Hit tester here
                        TouchCoord center = new TouchCoord();
                        center.setX((firstTap.getX() + secondTap.getX())/2);
                        center.setY((firstTap.getX() + secondTap.getX())/2);
                        mCallback.didReceiveDoubleTapForView(center);
                    }

                    singleTapTouchEvent = null;

                }
                break;

            case SDLPerformingTouchTypeNone:
                break;
        }

        previousTouchEvent = null;
        previousTouch = null;
        performingTouchType = SDLPerformingTouchType.SDLPerformingTouchTypeNone;

    }

    private void sdl_handleTouchCanceled(TouchEvent touchEvent){
        if (singleTapTimer !=null){
            singleTapTimer.cancel();
            singleTapTimer=null;
        }
        switch (performingTouchType){
            case SDLPerformingTouchTypeMultiTouch:
                sdl_setMultiTouchFingerTouchForTouch(touchEvent);
                if (currentPinchGesture.isValid()) {
                    //TODO: Add UI Hit tester here
                    mCallback.pinchCanceledAtCenterPoint(currentPinchGesture.getCenter());
                }
                currentPinchGesture = null;
                break;

            case SDLPerformingTouchTypePanningTouch:
                mCallback.panningCanceledAtPoint(touchEvent.getTouchCoordinates().get(0));
                break;

            case SDLPerformingTouchTypeSingleTouch: //Fallthrough

            case SDLPerformingTouchTypeNone:
                break;

        }
    }

    private void sdl_setMultiTouchFingerTouchForTouch(TouchEvent touchEvent){
        TouchCoord currentTouchCoord = touchEvent.getTouchCoordinates().get(0);
        switch (touchEvent.getId()){
            case 0: //First Finger
                currentPinchGesture.setFirstTouch(currentTouchCoord);
                break;
            case 1: //Second Finger
                currentPinchGesture.setSecondTouch(currentTouchCoord);
                break;
        }

    }

    private void sdl_initializeSingleTapTimerAtPoint(final TouchCoord point){
        singleTapTimer = new Timer();
        singleTapTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //TODO: Add UI Hit tester here
                mCallback.didReceiveSingleTapForView(point);
            }
        }, tapTimeThreshold);
    }

    private void sdl_cancelSingleTapTimer(){
        if (singleTapTimer==null){
            return;
        }
        singleTapTimer.cancel();
        singleTapTimer = null;
    }

}
