package com.aplika.avraham;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.accessibilityservice.AccessibilityService;
import java.util.*;

public class MainActivity extends Activity {
 LinearLayout root,chatList;
 ScrollView chatScroll;
 EditText input;
 TextView status;
 OfflineEngine engine;
 AudioManager audio;
 boolean english=false;

 final int BG=Color.rgb(248,247,251),TEXT=Color.rgb(43,42,52),MUTED=Color.rgb(111,109,122);
 final int BUBBLE=Color.WHITE,USER_BUBBLE=Color.rgb(236,232,252),BORDER=Color.rgb(226,222,235);

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(BG);
  getWindow().setNavigationBarColor(BG);
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  engine=new OfflineEngine(this);
  buildChatUi();
  addMessage("שלום. אני אברהם העברי. אני עובד אופליין ומהר, בלי מודל חיצוני.\nאפשר לכתוב לי בקשה רגילה או לבקש פעולה במכשיר.","assistant");
  status.setText("טוען מאגר מקומי...");
  new Thread(()->{ engine.loadChat(this); runOnUiThread(()->status.setText("אופליין • מוכן")); engine.loadCommands(this); engine.loadApps(this); },"catalog-loader").start();
 }

 TextView label(String s,float size,int color){
  TextView t=new TextView(this);
  t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");
  return t;
 }

 GradientDrawable shape(int color,float radius,int stroke){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);
  if(stroke>0)g.setStroke(1,stroke==1?BORDER:Color.TRANSPARENT);
  return g;
 }

 Button softButton(String s){
  Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);
  b.setTextColor(TEXT);b.setPadding(18,4,18,4);b.setMinHeight(42);b.setBackground(shape(Color.WHITE,26,1));
  return b;
 }

 void buildChatUi(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  root.setPadding(14,10,14,10);

  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(6,4,4,8);
  ImageView avatar=new ImageView(this);avatar.setImageResource(R.drawable.ic_avraham);
  top.addView(avatar,new LinearLayout.LayoutParams(48,48));
  LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.setPadding(10,0,0,0);
  TextView title=label("אברהם העברי",20,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  titleBox.addView(title);
  status=label("אופליין • מוכן",12,MUTED);titleBox.addView(status);
  top.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
  Button tools=softButton("כלים");tools.setOnClickListener(v->showTools());
  top.addView(tools,new LinearLayout.LayoutParams(82,44));
  root.addView(top);

  View line=new View(this);line.setBackgroundColor(BORDER);root.addView(line,new LinearLayout.LayoutParams(-1,1));

  chatScroll=new ScrollView(this);chatScroll.setFillViewport(true);chatScroll.setVerticalScrollBarEnabled(false);
  chatList=new LinearLayout(this);chatList.setOrientation(LinearLayout.VERTICAL);chatList.setPadding(3,12,3,12);
  chatScroll.addView(chatList,new ScrollView.LayoutParams(-1,-2));
  root.addView(chatScroll,new LinearLayout.LayoutParams(-1,0,1));

  LinearLayout suggestions=new LinearLayout(this);suggestions.setOrientation(LinearLayout.HORIZONTAL);suggestions.setGravity(Gravity.CENTER_VERTICAL);
  suggestions.setPadding(0,3,0,7);
  addSuggestion(suggestions,"פתח לי כרום");addSuggestion(suggestions,"תגביה שמע");addSuggestion(suggestions,"תעביר לשיר הבא");
  root.addView(suggestions,new LinearLayout.LayoutParams(-1,50));

  LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);composer.setPadding(0,4,0,0);
  input=new EditText(this);input.setTextSize(16);input.setTextColor(TEXT);input.setHintTextColor(Color.rgb(150,147,160));
  input.setHint("כתוב הודעה...");input.setSingleLine(false);input.setMaxLines(4);input.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  input.setPadding(18,10,18,10);input.setBackground(shape(Color.WHITE,28,1));
  composer.addView(input,new LinearLayout.LayoutParams(0,58,1));
  Button send=softButton("שלח");send.setTextColor(Color.WHITE);send.setBackground(shape(Color.rgb(109,94,245),28,0));
  send.setOnClickListener(v->sendCurrent());composer.addView(send,new LinearLayout.LayoutParams(78,58));
  root.addView(composer,new LinearLayout.LayoutParams(-1,64));

  setContentView(root);
  getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
 }

 void addSuggestion(LinearLayout box,String text){
  Button b=softButton(text);b.setTextSize(12);b.setOnClickListener(v->{input.setText(text);sendCurrent();});
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,44,1);lp.setMargins(3,0,3,0);box.addView(b,lp);
 }

 TextView bubble(String text,boolean user){
  TextView t=label(text,16,TEXT);t.setLineSpacing(0,1.12f);t.setPadding(16,12,16,12);t.setBackground(shape(user?USER_BUBBLE:BUBBLE,22,1));
  if(user)t.setTextColor(Color.rgb(48,43,72));
  return t;
 }

 void addMessage(String text,String who){
  boolean user="user".equals(who);
  LinearLayout row=new LinearLayout(this);row.setGravity(user?Gravity.RIGHT:Gravity.LEFT);
  row.setPadding(8,4,8,4);
  TextView b=bubble(text,user);
  LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(user?56:8,2,user?8:56,2);row.addView(b,bp);
  chatList.addView(row,new LinearLayout.LayoutParams(-1,-2));
  chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
 }

 void sendCurrent(){
  String q=input.getText().toString().trim();if(q.isEmpty())return;
  input.setText("");
  addMessage(q,"user");
  process(q);
 }

 void showTools(){
  PopupMenu p=new PopupMenu(this,findViewById(android.R.id.content));
  p.getMenu().add("צ׳אט");
  p.getMenu().add("פתיחת אפליקציה");
  p.getMenu().add("פעולת מערכת");
  p.getMenu().add("בחר תיקיית קבצים");
  p.setOnMenuItemClickListener(m->{String s=m.getTitle().toString();
   if(s.startsWith("בחר"))pickFolder();
   else if(s.startsWith("פתיחת")){addMessage("כתוב את שם האפליקציה שתרצה לפתוח.","assistant");}
   else if(s.startsWith("פעולת")){addMessage("כתוב את פעולת המערכת שתרצה לבצע.","assistant");}
   return true;
  });p.show();
 }

 String norm(String s){return OfflineEngine.normalize(s);}
 boolean hasAny(String q,String...words){String n=norm(q);for(String w:words)if(n.contains(norm(w)))return true;return false;}
 boolean openRequest(String q){return hasAny(q,"פתח","תפתח","לפתוח","פתיחה","open","launch","start","run");}
 boolean actionRequest(String q){return hasAny(q,"תגביה","תגביהה","תנמיך","השתק","נגן","השהה","עצור","הבא","קודם","חזור","אחורה","אחרונות","התראות","הגדרות מהירות","צלם מסך","צילום מסך","נעל מסך","נעילת מסך","volume","mute","play","pause","next","previous","home","back","notifications","quick settings","screenshot","lock screen");}
 boolean settingsRequest(String q){return hasAny(q,"wifi","wi-fi","רשת אלחוטית","וויפי","וייפיי","bluetooth","בלוטוס","בלוטות","מצב טיסה","airplane","נקודה חמה","hotspot","vpn","dns","תצוגה","display","notification settings","אחסון","storage","הרשאות","permissions","מיקום","location","מקלדת","keyboard","שפה","language","תאריך","date","שעה","time","nfc","שידור מסך","cast");}

 void process(String q){
  if(direct(q))return;
  if(settingsRequest(q)){runAction(q);return;}
  if(openRequest(q)){openThing(q);return;}
  if(actionRequest(q)){runAction(q);return;}
  chat(q);
 }

 void chat(String q){
  OfflineEngine.Resp fast=engine.quickResponse(q);
  if(fast!=null){addMessage(english?fast.en:fast.he,"assistant");return;}
  if(!engine.chatLoaded){
   new Thread(()->{engine.loadChat(this);runOnUiThread(()->chat(q));},"chat-loader").start();
   return;
  }
  OfflineEngine.Resp r=engine.bestResponse(q,english);
  if(r==null){addMessage(english?"I could not match that request yet. Try another wording with the main keyword.":"עדיין לא מצאתי התאמה טובה. נסה לנסח עם מילת המפתח העיקרית.","assistant");return;}
  addMessage(english?r.en:r.he,"assistant");
 }

 boolean media(int k){
  try{audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,k));audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,k));return true;}catch(Exception e){return false;}
 }
 boolean global(int action){return ShortcutService.doGlobal(action);}

 boolean direct(String q){
  String x=norm(q);
  if(hasAny(x,"פתח הגדרות","תפתח הגדרות","פתח את ההגדרות","תפתח את ההגדרות","היכנס להגדרות","open settings","settings") &&
     !hasAny(x,"הגדרות התראות","התראות אפליקציה","notification settings","wifi","wi-fi","רשת אלחוטית","bluetooth","בלוטוס","מצב טיסה","airplane","נקודה חמה","hotspot","vpn","מסך","תצוגה","display","אחסון","storage","הרשאות","permissions","מיקום","location","מקלדת","keyboard","שפה","language","תאריך","date","שעה","time","nfc","שידור מסך","cast")){
   try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));addMessage(english?"Opening Android settings.":"פותח את הגדרות Android.","assistant");}catch(Exception e){addMessage(english?"Could not open Android settings.":"לא הצלחתי לפתוח את הגדרות Android.","assistant");}
   return true;
  }
  if(hasAny(x,"בטל השתקה","unmute")){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_UNMUTE,0);addMessage(english?"Media unmuted.":"ההשתקה בוטלה.","assistant");return true;}
  if(hasAny(x,"השתק","השתקה","שקט","mute")){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_MUTE,0);addMessage(english?"Media muted.":"השמע הושתק.","assistant");return true;}
  if(hasAny(x,"תגביה","תגביהה","הגבהה","תגביר","תעלה","תרים","הגבר","volume up","increase volume","louder")){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0);addMessage(english?"Volume increased.":"עוצמת השמע הוגברה.","assistant");return true;}
  if(hasAny(x,"תנמיך","הנמכה","תוריד","תקטין","הנמך","volume down","decrease volume","quieter","lower volume")){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0);addMessage(english?"Volume decreased.":"עוצמת השמע הונמכה.","assistant");return true;}
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,3})(?:\\s*%)?").matcher(x);
  if(hasAny(x,"ווליום","עוצמה","volume","שמע","קול")&&m.find()){int p=Math.max(0,Math.min(100,Integer.parseInt(m.group(1))));int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);audio.setStreamVolume(AudioManager.STREAM_MUSIC,(max*p)/100,0);addMessage((english?"Volume set to ":"עוצמת השמע הוגדרה ל-")+p+"%","assistant");return true;}
  if(hasAny(x,"תעביר לשיר הבא","שיר הבא","הבא","next track","next song")&&hasAny(x,"שיר","מוזיקה","track","song")){if(media(KeyEvent.KEYCODE_MEDIA_NEXT)){addMessage(english?"Next track.":"השיר הבא.","assistant");return true;}}
  if(hasAny(x,"שיר קודם","שיר הקודם","previous track","previous song")&&hasAny(x,"שיר","מוזיקה","track","song")){if(media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)){addMessage(english?"Previous track.":"השיר הקודם.","assistant");return true;}}
  if(!openRequest(q) && hasAny(x,"נגן","נגינה","play music","play")){if(media(KeyEvent.KEYCODE_MEDIA_PLAY)){addMessage(english?"Play.":"ניגון.","assistant");return true;}}
  if(hasAny(x,"השהה","השהייה","pause")){if(media(KeyEvent.KEYCODE_MEDIA_PAUSE)){addMessage(english?"Paused.":"הושהה.","assistant");return true;}}
  if(hasAny(x,"עצור מוזיקה","stop music")){if(media(KeyEvent.KEYCODE_MEDIA_STOP)){addMessage(english?"Stopped.":"המוזיקה נעצרה.","assistant");return true;}}
  int g=-1;String msg=null;
  if(hasAny(x,"חזור הביתה","מסך הבית","דף הבית","home")){g=AccessibilityService.GLOBAL_ACTION_HOME;msg=english?"Home.":"מסך הבית.";}
  else if(hasAny(x,"חזור אחורה","אחורה","back")){g=AccessibilityService.GLOBAL_ACTION_BACK;msg=english?"Back.":"חזרה.";}
  else if(hasAny(x,"אחרונות","אפליקציות אחרונות","recents","recent apps")){g=AccessibilityService.GLOBAL_ACTION_RECENTS;msg=english?"Recent apps.":"האפליקציות האחרונות.";}
  else if(hasAny(x,"פתח התראות","התראות","notifications")){g=AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS;msg=english?"Notifications.":"התראות.";}
  else if(hasAny(x,"הגדרות מהירות","quick settings")){g=AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS;msg=english?"Quick settings.":"הגדרות מהירות.";}
  if(g>=0){if(global(g)){addMessage(msg,"assistant");return true;}addMessage(english?"Enable the accessibility service once for this system command.":"כדי לבצע את פקודת המערכת הזו ללא מגע, יש להפעיל פעם אחת את שירות הנגישות.","assistant");return true;}
  return false;
 }

 void runAction(String q){
  String x=norm(q);
  try{
   if(hasAny(x,"צילום מסך","צלם מסך","screenshot","take screenshot") && Build.VERSION.SDK_INT>=30 &&
      global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)){
    addMessage(english?"Screenshot taken.":"צילום המסך בוצע.","assistant");return;
   }
   if(hasAny(x,"נעל מסך","נעילת מסך","lock screen","lock device") && Build.VERSION.SDK_INT>=28 &&
      global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)){
    addMessage(english?"Screen locked.":"המסך ננעל.","assistant");return;
   }
   if(hasAny(x,"מסך הבית","דף הבית","חזור הביתה","home") && global(AccessibilityService.GLOBAL_ACTION_HOME)){
    addMessage(english?"Home.":"מסך הבית.","assistant");return;
   }
   if(hasAny(x,"חזור אחורה","אחורה","back") && global(AccessibilityService.GLOBAL_ACTION_BACK)){
    addMessage(english?"Back.":"חזרה.","assistant");return;
   }
   if(hasAny(x,"אפליקציות אחרונות","אחרונות","recents","recent apps") && global(AccessibilityService.GLOBAL_ACTION_RECENTS)){
    addMessage(english?"Recent apps.":"האפליקציות האחרונות.","assistant");return;
   }
   if(hasAny(x,"התראות","פתח התראות","notifications") && global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)){
    addMessage(english?"Notifications.":"התראות.","assistant");return;
   }
   if(hasAny(x,"הגדרות מהירות","quick settings") && global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)){
    addMessage(english?"Quick settings.":"הגדרות מהירות.","assistant");return;
   }
  }catch(Exception ignored){}

  int setting=settingForRequest(x);
  if(setting>=0){
   try{
    startActivity(engine.settingIntent(setting));
    addMessage((english?"Opening settings: ":"פותח הגדרות: ")+settingName(setting),"assistant");return;
   }catch(Exception ignored){}
  }

  addMessage(english?"I could not match that system action.":"לא הצלחתי לזהות את פעולת המערכת הזאת. נסה למשל Wi‑Fi, Bluetooth, צילום מסך, מסך הבית, אחורה או התראות.","assistant");
 }

 int settingForRequest(String x){
  if(hasAny(x,"wifi","wi fi","רשת אלחוטית","וויפי","וייפיי","אלחוטי"))return 0;
  if(hasAny(x,"bluetooth","בלוטוס","בלוטות","בלוטות'"))return 1;
  if(hasAny(x,"מצב טיסה","airplane"))return 2;
  if(hasAny(x,"רשת סלולרית","mobile network","cellular"))return 3;
  if(hasAny(x,"שימוש בנתונים","data usage","mobile data"))return 4;
  if(hasAny(x,"נקודה חמה","hotspot","mobile hotspot"))return 5;
  if(hasAny(x,"vpn"))return 6;
  if(hasAny(x,"dns פרטי","private dns"))return 7;
  if(hasAny(x,"תצוגה","display","בהירות","brightness"))return 8;
  if(hasAny(x,"צליל","שמע","sound"))return 9;
  if(hasAny(x,"התראות","notification settings","notification"))return 10;
  if(hasAny(x,"אפליקציות","apps settings","applications"))return 11;
  if(hasAny(x,"אחסון","storage"))return 12;
  if(hasAny(x,"אבטחה","security"))return 13;
  if(hasAny(x,"מיקום","location"))return 14;
  if(hasAny(x,"נגישות","accessibility"))return 15;
  if(hasAny(x,"סוללה","battery"))return 16;
  if(hasAny(x,"אופטימיזציית סוללה","battery optimization"))return 17;
  if(hasAny(x,"תאריך","date"))return 18;
  if(hasAny(x,"שעה","time"))return 19;
  if(hasAny(x,"אזור זמן","timezone","time zone"))return 20;
  if(hasAny(x,"שפה","language","locale"))return 21;
  if(hasAny(x,"מקלדת","keyboard","קלט"))return 22;
  if(hasAny(x,"שידור מסך","cast","casting"))return 23;
  if(hasAny(x,"nfc"))return 24;
  if(hasAny(x,"רשתות","wireless"))return 25;
  if(hasAny(x,"סנכרון","sync"))return 26;
  return -1;
 }

 String settingName(int i){
  String[] h={"Wi‑Fi","Bluetooth","מצב טיסה","רשת סלולרית","שימוש בנתונים","נקודה חמה","VPN","DNS פרטי","תצוגה","שמע","התראות","אפליקציות","אחסון","אבטחה","מיקום","נגישות","סוללה","אופטימיזציית סוללה","תאריך","שעה","אזור זמן","שפה","מקלדת","שידור מסך","NFC","רשתות","סנכרון"};
  return i>=0&&i<h.length?h[i]:"מערכת";
 }

 String targetOf(String q){
  String x=norm(q);
  String[] filler={"פתח","תפתח","לפתוח","פתיחה","לי","את","בבקשה","open","launch","start","run","please","app"};
  for(String w:filler)x=x.replaceAll("(?iu)(^| )"+java.util.regex.Pattern.quote(norm(w))+"(?= |$)"," ");
  return x.trim();
 }

 boolean openThing(String q){
  String target=targetOf(q);
  String wanted=engine.canonical(target);
  PackageManager pm=getPackageManager();

  Intent launcher=new Intent(Intent.ACTION_MAIN);
  launcher.addCategory(Intent.CATEGORY_LAUNCHER);
  List<ResolveInfo> launchers=pm.queryIntentActivities(launcher,PackageManager.MATCH_ALL);
  ResolveInfo best=null;String bn="";String bp="";int bs=0;

  for(ResolveInfo ri:launchers){
   if(ri.activityInfo==null)continue;
   CharSequence label=ri.loadLabel(pm);
   String name=label==null?"":label.toString();
   String pkg=ri.activityInfo.packageName==null?"":ri.activityInfo.packageName;
   int s=appSpecialScore(wanted,pkg);
   if(s==0 && wanted.equals(engine.canonical(name)))s=120;
   if(s==0)s=engine.score(target,name+" "+pkg.replace('.',' '));
   if(s>bs){bs=s;best=ri;bn=name;bp=pkg;}
  }

  ResolveInfo semantic=semanticApp(pm,target);
  if(semantic!=null && (best==null||bs<25)){
   best=semantic;bp=semantic.activityInfo.packageName;
   CharSequence label=semantic.loadLabel(pm);bn=label==null?"":label.toString();bs=90;
  }

  if(best!=null && bs>=10){
   try{
    Intent i=new Intent();
    i.setComponent(new ComponentName(bp,best.activityInfo.name));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(i);
    addMessage((english?"Opening ":"פותח ")+(bn.isEmpty()?target:bn),"assistant");return true;
   }catch(Exception ignored){}
  }

  String t=OfflineEngine.normalize(target);
  if(t.contains("קבצים")||t.contains("סייר")||t.contains("file manager")||t.contains("file explorer")||t.equals("files")){
   try{
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i,11);
    addMessage(english?"Opening the file browser.":"פותח את סייר הקבצים.","assistant");return true;
   }catch(Exception ignored){}
  }

  addMessage(english?"I could not find that installed app.":"לא מצאתי את האפליקציה הזו בין האפליקציות המותקנות.","assistant");
  return true;
 }

 int appSpecialScore(String w,String pkg){
  String p=pkg==null?"":pkg.toLowerCase(Locale.ROOT);
  if(w.equals("play store") && p.equals("com.android.vending"))return 150;
  if(w.equals("google drive") && p.contains("google.android.apps.docs"))return 150;
  if(w.contains("chrome") && p.equals("com.android.chrome"))return 150;
  if(w.contains("gmail") && p.equals("com.google.android.gm"))return 150;
  if((w.equals("google photos")||w.equals("gallery")) && p.contains("google.android.apps.photos"))return 150;
  if(w.equals("files") && (p.contains("google.android.apps.nbu.files")||p.contains("filemanager")||p.equals("com.google.android.documentsui")))return 140;
  if(w.equals("clock") && (p.contains("deskclock")||p.contains("clock")))return 140;
  if(w.equals("calculator") && p.contains("calculator"))return 140;
  if(w.equals("calendar") && p.contains("calendar"))return 140;
  return 0;
 }

 ResolveInfo semanticApp(PackageManager pm,String target){
  String t=OfflineEngine.normalize(target);
  String category=null;
  if(t.contains("דפדפן")||t.equals("browser")||t.contains("אינטרנט")||t.contains("chrome")||t.contains("כרום"))
   category=Intent.CATEGORY_APP_BROWSER;
  else if(t.contains("גלריה")||t.contains("תמונות")||t.contains("photos")||t.contains("gallery"))
   category=Intent.CATEGORY_APP_GALLERY;
  else if(t.contains("נגן")||t.contains("מוזיקה")||t.contains("music")||t.contains("media player"))
   category=Intent.CATEGORY_APP_MUSIC;
  else if(t.contains("מחשבון")||t.contains("calculator"))
   category=Intent.CATEGORY_APP_CALCULATOR;
  else if(t.contains("יומן")||t.contains("לוח שנה")||t.contains("calendar"))
   category=Intent.CATEGORY_APP_CALENDAR;
  else if(t.contains("מפות")||t.contains("maps")||t.contains("ניווט")||t.contains("navigation"))
   category=Intent.CATEGORY_APP_MAPS;
  else if(t.contains("דואר")||t.contains("מייל")||t.contains("email")||t.contains("gmail"))
   category=Intent.CATEGORY_APP_EMAIL;
  if(category==null)return null;
  try{
   Intent i=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,category);
   List<ResolveInfo> r=pm.queryIntentActivities(i,PackageManager.MATCH_ALL);
   return r.isEmpty()?null:r.get(0);
  }catch(Exception e){return null;}
 }

 void pickFolder(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  startActivityForResult(i,11);
 }

 @Override protected void onActivityResult(int r,int c,Intent d){
  super.onActivityResult(r,c,d);
  if(r==11&&c==RESULT_OK&&d!=null){
   getPreferences(MODE_PRIVATE).edit().putString("tree",d.getData().toString()).apply();
   try{getContentResolver().takePersistableUriPermission(d.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
   addMessage(english?"Folder saved for offline file search.":"התיקייה נשמרה לחיפוש קבצים באופליין.","assistant");
  }
 }
}