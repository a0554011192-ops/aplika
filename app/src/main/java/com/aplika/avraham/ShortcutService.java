package com.aplika.avraham;
import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

public class ShortcutService extends AccessibilityService{
 private static ShortcutService instance;
 private long plus=0;

 public void onServiceConnected(){super.onServiceConnected();instance=this;}
 public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}

 public static boolean doGlobal(int action){
  return instance!=null&&instance.performGlobalAction(action);
 }

 public interface ToggleCallback{void onResult();}

 public static boolean toggleQuickSetting(final ToggleCallback success,final ToggleCallback failure,final String... labels){
  final ShortcutService service=instance;
  if(service==null)return false;
  if(!service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS))return false;
  new Handler(Looper.getMainLooper()).postDelayed(()->service.tryClickQuickSetting(success,failure,labels,0),650);
  return true;
 }

 private void tryClickQuickSetting(final ToggleCallback success,final ToggleCallback failure,final String[] labels,final int attempt){
  if(instance!=this){if(failure!=null)failure.onResult();return;}
  AccessibilityNodeInfo root=getRootInActiveWindow();
  AccessibilityNodeInfo node=findClickableNode(root,labels);
  if(node!=null){
   boolean clicked=false;
   try{clicked=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);}catch(Exception ignored){}
   if(clicked){
    if(success!=null)success.onResult();
    new Handler(Looper.getMainLooper()).postDelayed(()->{try{if(instance==this)performGlobalAction(GLOBAL_ACTION_BACK);}catch(Exception ignored){}},180);
    return;
   }
  }
  if(attempt<3){
   new Handler(Looper.getMainLooper()).postDelayed(()->tryClickQuickSetting(success,failure,labels,attempt+1),300);
  }else if(failure!=null){
   failure.onResult();
  }
 }

 private AccessibilityNodeInfo findClickableNode(AccessibilityNodeInfo node,String... labels){
  if(node==null)return null;
  String text=node.getText()==null?"":node.getText().toString().toLowerCase(Locale.ROOT);
  String desc=node.getContentDescription()==null?"":node.getContentDescription().toString().toLowerCase(Locale.ROOT);
  boolean matches=false;
  for(String label:labels){
   String q=label.toLowerCase(Locale.ROOT);
   if(!q.isEmpty()&&(text.contains(q)||desc.contains(q))){matches=true;break;}
  }
  if(matches&&node.isClickable())return node;
  for(int i=0;i<node.getChildCount();i++){
   AccessibilityNodeInfo child=findClickableNode(node.getChild(i),labels);
   if(child!=null)return child;
  }
  return null;
 }

 public boolean onKeyEvent(KeyEvent e){
  if(e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return false;
  int k=e.getKeyCode();
  if(k==KeyEvent.KEYCODE_PLUS||k==KeyEvent.KEYCODE_EQUALS){plus=SystemClock.uptimeMillis();return true;}
  if(k==KeyEvent.KEYCODE_MINUS&&plus>0&&SystemClock.uptimeMillis()-plus<=1000){
   plus=0;
   Intent i=new Intent(this,MainActivity.class);
   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   startActivity(i);
   return true;
  }
  return false;
 }

 public void onAccessibilityEvent(AccessibilityEvent e){}
 public void onInterrupt(){}
}