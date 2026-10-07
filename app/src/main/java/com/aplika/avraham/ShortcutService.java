package com.aplika.avraham;
import android.accessibilityservice.AccessibilityService;import android.view.KeyEvent;import android.content.Intent;import android.os.SystemClock;
public class ShortcutService extends AccessibilityService{
 private long plus=0;
 public boolean onKeyEvent(KeyEvent e){if(e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return false;int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_PLUS||k==KeyEvent.KEYCODE_EQUALS){plus=SystemClock.uptimeMillis();return true;}if(k==KeyEvent.KEYCODE_MINUS&&plus>0&&SystemClock.uptimeMillis()-plus<=1000){plus=0;Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);return true;}return false;}
 public void onInterrupt(){}
}