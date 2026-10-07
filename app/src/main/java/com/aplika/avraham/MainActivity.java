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
 ArrayList<AppRow> installedApps=new ArrayList<>();
 volatile boolean installedAppsLoaded=false;
 SharedPreferences aliasPrefs;

 final int BG=Color.rgb(248,247,251),TEXT=Color.rgb(43,42,52),MUTED=Color.rgb(111,109,122);
 final int BUBBLE=Color.WHITE,USER_BUBBLE=Color.rgb(236,232,252),BORDER=Color.rgb(226,222,235);

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(BG);
  getWindow().setNavigationBarColor(BG);
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  engine=new OfflineEngine(this);
  aliasPrefs=getSharedPreferences("app_aliases",MODE_PRIVATE);
  buildChatUi();
  new Thread(()->loadInstalledApps(),"installed-app-loader").start();
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
  LinearLayout topButtons=new LinearLayout(this);topButtons.setGravity(Gravity.CENTER_VERTICAL);
  ImageButton settings=new ImageButton(this);settings.setImageResource(R.drawable.ic_settings);settings.setContentDescription("הגדרות");settings.setBackground(shape(Color.WHITE,24,1));settings.setPadding(11,11,11,11);settings.setOnClickListener(v->showAppManager());
  topButtons.addView(settings,new LinearLayout.LayoutParams(48,44));
  Button tools=softButton("כלים");tools.setOnClickListener(v->showTools());
  LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(78,44);tlp.setMargins(6,0,0,0);topButtons.addView(tools,tlp);
  top.addView(topButtons);
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

 static class AppRow{
  String label,packageName,activityName;
  boolean system,launchable;
  AppRow(String l,String p,String a,boolean s,boolean z){label=l;packageName=p;activityName=a;system=s;launchable=z;}
 }

 void loadInstalledApps(){
  try{
   PackageManager pm=getPackageManager();
   HashMap<String,String> launcherActivities=new HashMap<>();
   Intent launcher=new Intent(Intent.ACTION_MAIN);launcher.addCategory(Intent.CATEGORY_LAUNCHER);
   for(ResolveInfo ri:pm.queryIntentActivities(launcher,PackageManager.MATCH_ALL)){
    if(ri.activityInfo!=null)launcherActivities.put(ri.activityInfo.packageName,ri.activityInfo.name);
   }
   ArrayList<AppRow> out=new ArrayList<>();
   for(ApplicationInfo ai:pm.getInstalledApplications(PackageManager.MATCH_ALL)){
    String pkg=ai.packageName==null?"":ai.packageName;
    if(pkg.isEmpty())continue;
    CharSequence cs=ai.loadLabel(pm);
    String label=cs==null?pkg:cs.toString();
    out.add(new AppRow(label,pkg,launcherActivities.get(pkg),
      (ai.flags & ApplicationInfo.FLAG_SYSTEM)!=0,launcherActivities.containsKey(pkg)));
   }
   Collections.sort(out,(a,b)->a.label.compareToIgnoreCase(b.label));
   installedApps=out;installedAppsLoaded=true;
  }catch(Exception ignored){}
 }

 void ensureInstalledApps(Runnable done){
  if(installedAppsLoaded){done.run();return;}
  new Thread(()->{
   loadInstalledApps();
   runOnUiThread(done);
  },"installed-app-loader-retry").start();
 }

 String userAliases(String pkg){
  return aliasPrefs==null?"":aliasPrefs.getString(pkg,"");
 }

 String findCustomCommandPackage(String query){
  String q=OfflineEngine.normalize(query);
  if(q.isEmpty()||aliasPrefs==null)return null;
  for(Map.Entry<String,?> e:aliasPrefs.getAll().entrySet()){
   Object value=e.getValue();if(!(value instanceof String))continue;
   for(String a:((String)value).split("\\|")){
    String x=OfflineEngine.normalize(a);
    if(!x.isEmpty()&&(q.equals(x)||cleanUserCommand(q).equals(x)))return e.getKey();
   }
  }
  return null;
 }

 String findUserAliasPackage(String query){
  String q=OfflineEngine.normalize(query);
  if(q.isEmpty()||aliasPrefs==null)return null;
  for(Map.Entry<String,?> e:aliasPrefs.getAll().entrySet()){
   Object value=e.getValue();
   if(!(value instanceof String))continue;
   String raw=(String)value;
   for(String a:raw.split("\\|")){
    String x=OfflineEngine.normalize(a);
    if(x.isEmpty())continue;
    if(q.equals(x)||engine.score(q,x)>=35)return e.getKey();
   }
  }
  return null;
 }

 String cleanUserCommand(String raw){
  String x=OfflineEngine.normalize(raw);
  if(x.isEmpty())return "";
  x=x.replaceAll("^(תפתח|פתח|לפתוח|פתיחה|open|launch|start|run)\\s+","");
  x=x.replaceAll("\\b(לי|את|האפליקציה|אפליקציה|אפליקציית|שלי)\\b"," ").replaceAll("\\s+"," ").trim();
  return x;
 }

 void saveAliases(AppRow row,String raw){
  String clean=raw==null?"":raw.trim();
  if(clean.isEmpty()){aliasPrefs.edit().remove(row.packageName).apply();return;}
  LinkedHashSet<String> set=new LinkedHashSet<>();
  for(String a:clean.split("[,;|\\n]+")){
   String x=OfflineEngine.normalize(a);
   if(!x.isEmpty()){set.add(x);String y=cleanUserCommand(x);if(!y.isEmpty())set.add(y);}
  }
  aliasPrefs.edit().putString(row.packageName,String.join("|",set)).apply();
 }

 void showAppManager(){
  ensureInstalledApps(()->buildAppManagerDialog());
 }

 void buildAppManagerDialog(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,18,24,12);
  TextView count=label("נטענו "+installedApps.size()+" אפליקציות מהמכשיר",16,TEXT);
  count.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(count);
  TextView info=label("בחר אפליקציה, כתוב פקודה בעברית שתפתח אותה, ושמור. הפקודה נשמרת במכשיר ותעבוד גם אחרי הפעלה מחדש.",13,MUTED);
  info.setPadding(0,6,0,12);box.addView(info);

  LinearLayout actions=new LinearLayout(this);
  Button roles=softButton("בדיקת Android");roles.setOnClickListener(v->showAndroidRoles());
  Button refresh=softButton("רענן");refresh.setOnClickListener(v->{loadInstalledApps();count.setText("נטענו "+installedApps.size()+" אפליקציות מהמכשיר");});
  actions.addView(roles,new LinearLayout.LayoutParams(0,44,1));
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,44,1);rp.setMargins(8,0,0,0);actions.addView(refresh,rp);
  box.addView(actions);

  EditText search=new EditText(this);search.setHint("חפש אפליקציה או חבילה...");search.setSingleLine(true);
  search.setTextSize(15);search.setPadding(16,8,16,8);search.setBackground(shape(Color.WHITE,22,1));
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,50);sp.setMargins(0,12,0,10);box.addView(search,sp);

  ListView list=new ListView(this);list.setDividerHeight(1);
  AppAdapter adapter=new AppAdapter(installedApps);list.setAdapter(adapter);
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int st,int c,int a){}
   public void onTextChanged(CharSequence s,int st,int b,int c){adapter.filter(s.toString());}
   public void afterTextChanged(android.text.Editable e){}
  });
  box.addView(list,new LinearLayout.LayoutParams(-1,0,1));

  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("יצירת פקודות לאפליקציות").setView(box).setNegativeButton("סגור",null).create();
  dialog.setOnShowListener(v->{
   Window w=dialog.getWindow();
   if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94f),(int)(getResources().getDisplayMetrics().heightPixels*0.88f));
  });
  dialog.show();
 }

 class AppAdapter extends BaseAdapter{
  ArrayList<AppRow> all,shown;
  AppAdapter(ArrayList<AppRow> a){all=new ArrayList<>(a);shown=new ArrayList<>(a);}
  public int getCount(){return shown.size();}
  public Object getItem(int i){return shown.get(i);}
  public long getItemId(int i){return i;}
  public View getView(int pos,View convert,android.view.ViewGroup parent){
   AppRow row=shown.get(pos);
   LinearLayout item=new LinearLayout(MainActivity.this);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(10,10,6,10);
   LinearLayout texts=new LinearLayout(MainActivity.this);texts.setOrientation(LinearLayout.VERTICAL);
   TextView name=label(row.label,15,TEXT);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);texts.addView(name);
   String a=userAliases(row.packageName);
   String sub=row.packageName+(row.launchable?"":" • ללא מסך פתיחה");
   if(!a.isEmpty())sub+="\nפקודה: "+a.replace("|"," , ");
   TextView pkg=label(sub,11,MUTED);pkg.setPadding(0,4,0,0);texts.addView(pkg);
   item.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
   Button edit=softButton(a.isEmpty()?"פקודה":"ערוך");edit.setTextSize(12);
   edit.setOnClickListener(v->showAliasEditor(row));
   item.addView(edit,new LinearLayout.LayoutParams(70,42));
   item.setOnClickListener(v->showAliasEditor(row));
   return item;
  }
  void filter(String q){
   String x=OfflineEngine.normalize(q);shown.clear();
   if(x.isEmpty())shown.addAll(all);
   else{
    for(AppRow r:all){
     if(OfflineEngine.normalize(r.label).contains(x)||OfflineEngine.normalize(r.packageName).contains(x)||userAliases(r.packageName).contains(x))shown.add(r);
    }
   }
   notifyDataSetChanged();
  }
 }

 void showAliasEditor(AppRow row){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,8,24,4);
  TextView app=label(row.label+"\n"+row.packageName,15,TEXT);box.addView(app);
  EditText alias= new EditText(this);alias.setHint("למשל: תפתח את המחשבון שלי");alias.setText(userAliases(row.packageName).replace("|",", "));alias.setSingleLine(false);alias.setMaxLines(3);
  alias.setPadding(14,10,14,10);box.addView(alias,new LinearLayout.LayoutParams(-1,70));
  new AlertDialog.Builder(this).setTitle("יצירת פקודה").setView(box)
   .setPositiveButton("שמור", (d,w)->{saveAliases(row,alias.getText().toString());})
    .setNeutralButton("מחק פקודה", (d,w)->{aliasPrefs.edit().remove(row.packageName).apply();})
   .setNegativeButton("ביטול",null).show();
 }

 void showAndroidRoles(){
  ensureInstalledApps(()->{
   String[] names={"דפדפן","מחשבון","יומן","אנשי קשר","דואר","קבצים","גלריה","מפות","חנות אפליקציות","הודעות","מוזיקה","מזג אוויר","בית","מצלמה","טלפון","שעון","הגדרות"};
   String[] cats={Intent.CATEGORY_APP_BROWSER,Intent.CATEGORY_APP_CALCULATOR,Intent.CATEGORY_APP_CALENDAR,Intent.CATEGORY_APP_CONTACTS,Intent.CATEGORY_APP_EMAIL,Intent.CATEGORY_APP_FILES,Intent.CATEGORY_APP_GALLERY,Intent.CATEGORY_APP_MAPS,Intent.CATEGORY_APP_MARKET,Intent.CATEGORY_APP_MESSAGING,Intent.CATEGORY_APP_MUSIC,Intent.CATEGORY_APP_WEATHER,null,null,null,null,null};
   StringBuilder sb=new StringBuilder();
   PackageManager pm=getPackageManager();
   for(int i=0;i<names.length;i++){
    String found=null;
    try{
     if(cats[i]!=null){
      if(cats[i].equals(Intent.CATEGORY_APP_FILES) && Build.VERSION.SDK_INT<29){
       found="לא זמין בגרסה זו";
      }else{
       Intent in=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,cats[i]);
       List<ResolveInfo> rs=pm.queryIntentActivities(in,PackageManager.MATCH_ALL);
       if(!rs.isEmpty())found=labelOf(rs.get(0));
      }
     }else if(i==12){
      Intent in=new Intent(Intent.ACTION_MAIN);in.addCategory(Intent.CATEGORY_HOME);
      List<ResolveInfo> rs=pm.queryIntentActivities(in,PackageManager.MATCH_ALL);
      if(!rs.isEmpty())found=labelOf(rs.get(0));
     }else if(i==13){
      List<ResolveInfo> rs=pm.queryIntentActivities(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),PackageManager.MATCH_ALL);
      if(!rs.isEmpty())found=labelOf(rs.get(0));
     }else if(i==14){
      List<ResolveInfo> rs=pm.queryIntentActivities(new Intent(Intent.ACTION_DIAL),PackageManager.MATCH_ALL);
      if(!rs.isEmpty())found=labelOf(rs.get(0));
     }else if(i==15){
      for(AppRow row:installedApps){
       String z=(row.label+" "+row.packageName).toLowerCase(Locale.ROOT);
       if(z.contains("clock")||z.contains("deskclock")||OfflineEngine.normalize(row.label).equals("שעון")){
        found=row.label;break;
       }
      }
     }else if(i==16){
      Intent in=new Intent(android.provider.Settings.ACTION_SETTINGS);
      List<ResolveInfo> rs=pm.queryIntentActivities(in,PackageManager.MATCH_ALL);
      found=rs.isEmpty()?"Android Settings":"Android Settings";
     }
    }catch(Exception ignored){}
    sb.append(names[i]).append(": ").append(found==null?"לא נמצא":found).append("\n");
   }
   sb.append("\nהערה: Android לא משתמש באותה אפליקציית ברירת מחדל בכל יצרן. הבדיקה בודקת את התפקיד שהמכשיר הנוכחי חושף.");
   new AlertDialog.Builder(this).setTitle("בדיקת אפליקציות/תפקידי Android").setMessage(sb.toString()).setPositiveButton("סגור",null).show();
  });
 }

 String labelOf(ResolveInfo r){
  try{CharSequence s=r.loadLabel(getPackageManager());return s==null?r.activityInfo.packageName:s.toString();}
  catch(Exception e){return r.activityInfo==null?"":r.activityInfo.packageName;}
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
 boolean actionRequest(String q){
  String x=norm(q);
  if(hasAny(x,"תגביה","תגביהה","תנמיך","השתק","השתקה","נגן","השהה","עצור","חזור אחורה","חזור הביתה","אחורה","אפליקציות אחרונות","אחרונות","פתח התראות","התראות","הגדרות מהירות","צלם מסך","צילום מסך","נעל מסך","נעילת מסך","volume","mute","play","pause","home","back","notifications","quick settings","screenshot","lock screen"))return true;
  return hasAny(x,"הבא","קודם","next","previous")&&hasAny(x,"שיר","מוזיקה","track","song");
 }
 boolean likelyActionCommand(String q){
  String x=norm(q);
  return hasAny(x,
   "פתח","תפתח","לפתוח","הצג","בדוק","נהל","כוון","בחר","עבור","היכנס","גישה",
   "הפעל","תפעיל","השבת","תכבה","אפשר","בטל","אפס","שנה","הגדר","שלוט","התאם",
   "נווט","עיין","עדכן","שמור","צלם","נעל","השתק","נגן","השהה","עצור","תגביה","תנמיך",
   "open","show","check","manage","adjust","choose","go","enter","access","enable","disable",
   "allow","reset","change","set","control","navigate","inspect","update","save","screenshot",
   "lock","mute","play","pause","stop","volume","quick settings"
  );
 }
 boolean settingsRequest(String q){
  String x=norm(q);
  if(!hasAny(x,"wifi","wi-fi","רשת אלחוטית","ויפי","וויפיי","וייפיי","bluetooth","בלוטוס","בלוטות","מצב טיסה","airplane","נקודה חמה","hotspot","vpn","dns","תצוגה","display","notification settings","אחסון","storage","הרשאות","permissions","מיקום","location","מקלדת","keyboard","שפה","language","תאריך","date","שעה","time","nfc","שידור מסך","cast","sound","שמע"))return false;
  if(openRequest(q)||actionRequest(q))return true;
  return x.equals("wifi")||x.equals("wi fi")||x.equals("ויפי")||x.equals("וויפיי")||x.equals("וייפיי")||
         x.equals("bluetooth")||x.equals("בלוטוס")||x.equals("בלוטות")||x.equals("מצב טיסה")||
         x.equals("airplane")||x.equals("נקודה חמה")||x.equals("hotspot")||x.equals("vpn")||
         x.equals("dns")||x.equals("תצוגה")||x.equals("display")||x.equals("אחסון")||x.equals("storage")||
         x.equals("הרשאות")||x.equals("permissions")||x.equals("מיקום")||x.equals("location")||
         x.equals("מקלדת")||x.equals("keyboard")||x.equals("שפה")||x.equals("language")||
         x.equals("תאריך")||x.equals("date")||x.equals("nfc")||x.equals("שידור מסך")||x.equals("cast")||
         x.equals("sound")||x.equals("שמע");
 }

 void process(String q){
  // Resolve deterministic Android commands before the large offline catalogs.
  // This prevents a known system command from being mistaken for ordinary chat.
  if(mathRequest(q))return;
  if(systemToggle(q))return;
  if(coreAndroidCommand(q))return;
  String customPkg=findCustomCommandPackage(q);if(customPkg!=null&&launchPackage(customPkg,q))return;
  if(direct(q))return;
  if(settingsRequest(q)){runAction(q);return;}
  if(openRequest(q)){openThing(q);return;}
  if(actionRequest(q)){runAction(q);return;}
  if(engine.commandsLoaded && likelyActionCommand(q) && engine.bestAction(q)!=null){runAction(q);return;}

  // Natural Android commands: a user should be able to say just "מחשבון",
  // "שעון", "דרייב", "גוגל פליי", etc. without adding the word "פתח".
  if(appNameOnlyRequest(q)){openThing(q);return;}

  chat(q);
 }

 boolean appNameOnlyRequest(String q){
  String x=norm(q);
  if(x.isEmpty())return false;

  // Core Android apps and common aliases. These are intentional exact/phrase
  // matches so ordinary chat sentences are not accidentally treated as apps.
  if(hasAny(x,
    "מחשבון","calculator","שעון","clock","דרייב","google drive",
    "גוגל פליי","גוגל פלי","google play","play store","חנות","חנות play",
    "סייר קבצים","מנהל קבצים","קבצים","files","file manager","file explorer",
    "גלריה","gallery","תמונות","google photos","photos",
    "מצלמה","camera","טלפון","phone","חייגן","dialer",
    "הודעות","messages","sms","אנשי קשר","contacts","contact",
    "יומן","לוח שנה","calendar","דפדפן","browser","אינטרנט",
    "גוגל","google","מפות","google maps","maps",
    "גימייל","gmail","דואר","email","יוטיוב","youtube",
    "כרום","chrome","ווטסאפ","וואטסאפ","whatsapp",
    "טלגרם","telegram","ספוטיפיי","spotify")){
   return true;
  }

  engine.loadApps(this);
  if(engine.isKnownAppAlias(x))return true;
  if(findCustomCommandPackage(x)!=null)return true;
  if(findUserAliasPackage(x)!=null)return true;

  // Any short phrase that exactly/closely matches a currently installed app
  // is also considered an app command, even when it isn't in the offline catalog.
  if(installedAppsLoaded && tokenCount(x)<=6){
   return bestInstalledScore(x)>=45;
  }
  return false;
 }

 int tokenCount(String x){return x.trim().isEmpty()?0:x.trim().split("\\s+").length;}

 int bestInstalledScore(String target){
  String q=OfflineEngine.normalize(target);
  int best=0;
  for(AppRow row:installedApps){
   String aliases=row.label+" "+row.packageName;
   int s=engine.score(q,aliases);
   if(engine.canonical(q).equals(engine.canonical(row.label)))s=Math.max(s,120);
   best=Math.max(best,s);
  }
  return best;
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

 boolean mathRequest(String raw){
  String x=raw==null?"":raw.trim();if(x.isEmpty())return false;
  String n=x.toLowerCase(Locale.ROOT).replace("×","*").replace("÷","/");
  n=n.replaceAll("(?i)\\bplus\\b","+").replaceAll("(?i)\\bminus\\b","-").replaceAll("(?i)\\btimes\\b","*").replaceAll("(?i)\\bdivided by\\b","/");
  n=n.replace("ועוד","+").replace("פלוס","+").replace("פחות","-").replace("כפול","*").replace("חלקי","/").replace("לחלק ב","/").replace("לחלק","/");
  n=n.replaceAll("(?i)כמה זה"," ").replaceAll("(?i)מה יוצא"," ").replaceAll("(?i)חשב"," ").replaceAll("="," ");
  if(!n.matches(".*\\d.*")||!n.matches("[\\d\\s+*/().,%.-]+"))return false;
  try{
   double v=new MathParser(n).parse();if(Double.isNaN(v)||Double.isInfinite(v)||Math.abs(v)>1e15)return false;
   String out;if(Math.abs(v-Math.rint(v))<1e-10)out=Long.toString(Math.round(v));else out=String.format(Locale.getDefault(),"%.10f",v).replaceAll("0+$","").replaceAll("[.,]$","");
   addMessage("התוצאה היא "+out+".","assistant");return true;
  }catch(Exception e){return false;}
 }
 static class MathParser{
  final String s;int p=0;MathParser(String x){s=x.replaceAll("\\s+","");}
  double parse(){double v=expr();if(p<s.length())throw new IllegalArgumentException();return v;}
  double expr(){double v=term();while(p<s.length()){char c=s.charAt(p);if(c=='+'){p++;v+=term();}else if(c=='-'){p++;v-=term();}else break;}return v;}
  double term(){double v=factor();while(p<s.length()){char c=s.charAt(p);if(c=='*'){p++;v*=factor();}else if(c=='/'){p++;double d=factor();if(Math.abs(d)<1e-15)throw new ArithmeticException();v/=d;}else break;}return v;}
  double factor(){if(p>=s.length())throw new IllegalArgumentException();char c=s.charAt(p);if(c=='+'){p++;return factor();}if(c=='-'){p++;return -factor();}if(c=='('){p++;double v=expr();if(p>=s.length()||s.charAt(p)!=')')throw new IllegalArgumentException();p++;return v;}int st=p;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'||s.charAt(p)==','))p++;if(st==p)throw new IllegalArgumentException();return Double.parseDouble(s.substring(st,p).replace(',','.'));}
 }

 boolean coreAndroidCommand(String q){
  String x=norm(q);
  if(x.isEmpty())return false;

  // Current time is an answer, not an app/settings command.
  if(hasAny(x,"מה השעה","מה השעה עכשיו","השעה","מה הזמן","what time is it","what's the time","current time")){
   String time=new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(new java.util.Date());
   addMessage(english?"The time is "+time+".":"השעה עכשיו "+time+".","assistant");
   return true;
  }

  // Android Home must work with both "מסך בית" and "מסך הבית".
  if(hasAny(x,"מסך בית","מסך הבית","דף בית","דף הבית","בית","home") &&
     (!hasAny(x,"הגדרות","settings") || isHomeCommand(x))){
   if(global(AccessibilityService.GLOBAL_ACTION_HOME)){
    addMessage(english?"Home.":"מסך הבית.","assistant");return true;
   }
   try{
    Intent home=new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(home);
    addMessage(english?"Home.":"מסך הבית.","assistant");
    return true;
   }catch(Exception ignored){}
  }

  // Core apps are resolved directly from Android roles/package names before
  // the 4,000-entry catalog. This is device-first and does not depend on a
  // particular manufacturer or a particular app being in the catalog.
  if(openRequest(x)){
   String target=targetOf(x);
   if(coreAppTarget(target))return true;
  }
  if(appNameOnlyRequestCore(x)){
   if(coreAppTarget(x))return true;
  }
  return false;
 }

 boolean appNameOnlyRequestCore(String q){
  String x=norm(q);
  if(x.isEmpty() || tokenCount(x)>5)return false;
  return hasAny(x,"מחשבון","calculator","שעון","clock","סייר קבצים","מנהל קבצים",
    "סייר הקבצים","קבצים","files","file manager","file explorer","גלריה","gallery",
    "מצלמה","camera","יומן","לוח שנה","calendar","טלפון","phone","חייגן","dialer",
    "הודעות","messages","אנשי קשר","contacts","דפדפן","browser","אינטרנט","browser",
    "מפות","maps","חנות","חנות play","google play","play store");
 }

 boolean coreAppTarget(String raw){
  String w=OfflineEngine.normalize(engine.canonical(raw));
  PackageManager pm=getPackageManager();
  if(w.isEmpty())return false;

  // Exact well-known package mappings first.
  String[] packages=null;
  if(w.equals("chrome")||w.equals("google chrome"))packages=new String[]{"com.android.chrome"};
  else if(w.equals("calculator"))packages=new String[]{"com.google.android.calculator","com.android.calculator2"};
  else if(w.equals("play store")||w.equals("google play"))packages=new String[]{"com.android.vending"};
  else if(w.equals("google drive"))packages=new String[]{"com.google.android.apps.docs"};
  else if(w.equals("gmail"))packages=new String[]{"com.google.android.gm"};
  else if(w.equals("google maps")||w.equals("maps"))packages=new String[]{"com.google.android.apps.maps"};
  else if(w.equals("youtube"))packages=new String[]{"com.google.android.youtube"};
  else if(w.equals("google photos"))packages=new String[]{"com.google.android.apps.photos"};
  else if(w.equals("whatsapp"))packages=new String[]{"com.whatsapp"};
  else if(w.equals("telegram"))packages=new String[]{"org.telegram.messenger"};
  else if(w.equals("spotify"))packages=new String[]{"com.spotify.music"};

  if(packages!=null){
   for(String pkg:packages)if(launchPackage(pkg,raw))return true;
  }

  // Android role/category resolution is more reliable than hard-coded vendor
  // package names for Calculator, Files, Gallery, Browser, Calendar, etc.
  String category=null;
  if(w.equals("calculator"))category=Intent.CATEGORY_APP_CALCULATOR;
  else if(w.equals("files"))category=Intent.CATEGORY_APP_FILES;
  else if(w.equals("gallery"))category=Intent.CATEGORY_APP_GALLERY;
  else if(w.equals("browser"))category=Intent.CATEGORY_APP_BROWSER;
  else if(w.equals("calendar"))category=Intent.CATEGORY_APP_CALENDAR;
  else if(w.equals("contacts"))category=Intent.CATEGORY_APP_CONTACTS;
  else if(w.equals("email"))category=Intent.CATEGORY_APP_EMAIL;
  else if(w.equals("maps"))category=Intent.CATEGORY_APP_MAPS;
  else if(w.equals("messages"))category=Intent.CATEGORY_APP_MESSAGING;
  else if(w.equals("music"))category=Intent.CATEGORY_APP_MUSIC;

  if(category!=null){
   try{
    Intent selector=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,category);
    List<ResolveInfo> rs=pm.queryIntentActivities(selector,PackageManager.MATCH_ALL);
    if(!rs.isEmpty()){
     ResolveInfo ri=rs.get(0);
     Intent launch=new Intent();
     launch.setComponent(new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name));
     launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     startActivity(launch);
     addMessage((english?"Opening ":"פותח ")+raw,"assistant");
     return true;
    }
   }catch(Exception ignored){}
  }

  // Clock apps are not represented by one universal Android CATEGORY.
  if(w.equals("clock")){
   try{
    Intent alarms=new Intent("android.intent.action.SHOW_ALARMS");
    alarms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if(pm.resolveActivity(alarms,PackageManager.MATCH_ALL)!=null){
     startActivity(alarms);
     addMessage((english?"Opening ":"פותח ")+raw,"assistant");
     return true;
    }
   }catch(Exception ignored){}
   for(AppRow row:installedApps){
    String z=OfflineEngine.normalize(row.label+" "+row.packageName);
    if(z.contains("clock")||z.contains("שעון")||z.contains("deskclock")){
     if(launchPackage(row.packageName,raw))return true;
    }
   }
  }

  // Files: if the OEM exposes no dedicated Files app, use Android's built-in
  // document tree so "סייר קבצים" still performs a real file-browsing action.
  if(w.equals("files")){
   try{
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i,11);
    addMessage(english?"Opening the file browser.":"פותח את סייר הקבצים.","assistant");
    return true;
   }catch(Exception ignored){}
  }
  return false;
 }

 boolean isHomeCommand(String q){
  String x=norm(q);
  if(x.equals("בית")||x.equals("מסך בית")||x.equals("מסך הבית")||x.equals("דף בית")||x.equals("דף הבית")||x.equals("home"))return true;
  return x.matches("^(פתח|תפתח|לפתוח|launch|open|start)(?: את)? (בית|מסך בית|מסך הבית|דף בית|דף הבית|home)$");
 }

 boolean quickToggle(String... labels){try{return ShortcutService.toggleQuickSetting(labels);}catch(Exception e){return false;}}
 boolean openSetting(String action,String ok){try{Intent i=new Intent(action);if(i.resolveActivity(getPackageManager())==null)return false;startActivity(i);addMessage(ok,"assistant");return true;}catch(Exception e){return false;}}
 boolean systemToggle(String q){
  String x=norm(q);boolean on=hasAny(x,"תפעיל","הפעל","להפעיל","הדלק","שים","עבור למצב","תעביר אותי למצב","תעביר אותי למצב טיסה","שים במצב טיסה","turn on","enable");boolean off=hasAny(x,"תכבה","כבה","לכבות","כיבוי","turn off","disable");if(!on&&!off)return false;
  if(hasAny(x,"בלוטוס","בלוטות","bluetooth")){if(quickToggle("bluetooth","בלוטוס","בלוטות")){addMessage(on?"הבלוטוס הופעל.":"הבלוטוס כובה.","assistant");return true;}if(on)try{Intent i=new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);startActivity(i);addMessage("פתחתי את בקשת הפעלת הבלוטוס.","assistant");return true;}catch(Exception ignored){}if(openSetting(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,"פתחתי את הגדרות הבלוטוס."))return true;}
  if(hasAny(x,"ויפי","וויפיי","וייפיי","wifi","wi fi","רשת אלחוטית")){if(quickToggle("wifi","wi-fi","wi fi","ויפי","וויפיי","וייפיי")){addMessage(on?"ה־Wi‑Fi הופעל.":"ה־Wi‑Fi כובה.","assistant");return true;}if(openSetting(android.provider.Settings.ACTION_WIFI_SETTINGS,"פתחתי את הגדרות ה־Wi‑Fi."))return true;}
  if(hasAny(x,"מצב טיסה","airplane")){if(quickToggle("airplane","airplane mode","מצב טיסה")){addMessage(on?"מצב טיסה הופעל.":"מצב טיסה כובה.","assistant");return true;}if(openSetting(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS,"פתחתי את הגדרות מצב הטיסה."))return true;}
  return false;
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
  // Deterministic Android home command.
  if(isHomeCommand(q)){
   if(global(AccessibilityService.GLOBAL_ACTION_HOME)){addMessage(english?"Home.":"מסך הבית.","assistant");return true;}
   try{
    Intent home=new Intent(Intent.ACTION_MAIN);home.addCategory(Intent.CATEGORY_HOME);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(home);addMessage(english?"Home.":"מסך הבית.","assistant");return true;
   }catch(Exception ignored){}
  }

  // Explicit common-app resolution happens before fuzzy matching.
  if(openRequest(q)){
   String t=targetOf(q);
   if(launchKnownApp(t)){return true;}
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
  if(hasAny(x,"חזור הביתה","מסך הבית","דף הבית","בית","home")){g=AccessibilityService.GLOBAL_ACTION_HOME;msg=english?"Home.":"מסך הבית.";}
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

  if(!engine.commandsLoaded){
   addMessage(english?"Loading system command catalog...":"טוען מאגר פקודות מערכת...","assistant");
   new Thread(()->{engine.loadCommands(this);runOnUiThread(()->runAction(q));},"commands-loader").start();
   return;
  }
  OfflineEngine.ActionEntry matched=engine.bestAction(q);
  if(matched!=null&&executeActionCode(matched.code,matched.setting,q))return;

  int setting=settingForRequest(x);
  if(setting>=0){
   try{
    startActivity(engine.settingIntent(setting));
    addMessage((english?"Opening settings: ":"פותח הגדרות: ")+settingName(setting),"assistant");return;
   }catch(Exception ignored){}
  }

  addMessage(english?"I could not match that system action.":"לא הצלחתי לזהות את פעולת המערכת הזאת. נסה למשל Wi‑Fi, Bluetooth, צילום מסך, מסך הבית, אחורה או התראות.","assistant");
 }

 boolean executeActionCode(String code,int setting,String q){
  if(code==null)return false;
  if("VOL_UP".equals(code)){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0);addMessage("עוצמת השמע הוגברה.","assistant");return true;}
  if("VOL_DOWN".equals(code)){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0);addMessage("עוצמת השמע הונמכה.","assistant");return true;}
  if("MUTE".equals(code)){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_MUTE,0);addMessage("השמע הושתק.","assistant");return true;}
  if("UNMUTE".equals(code)){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_UNMUTE,0);addMessage("ההשתקה בוטלה.","assistant");return true;}
  if("MEDIA_NEXT".equals(code)){if(media(KeyEvent.KEYCODE_MEDIA_NEXT)){addMessage("השיר הבא.","assistant");return true;}}
  if("MEDIA_PREV".equals(code)){if(media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)){addMessage("השיר הקודם.","assistant");return true;}}
  if("MEDIA_PLAY".equals(code)){if(media(KeyEvent.KEYCODE_MEDIA_PLAY)){addMessage("ניגון.","assistant");return true;}}
  if("MEDIA_PAUSE".equals(code)){if(media(KeyEvent.KEYCODE_MEDIA_PAUSE)){addMessage("הושהה.","assistant");return true;}}
  if("MEDIA_STOP".equals(code)){if(media(KeyEvent.KEYCODE_MEDIA_STOP)){addMessage("המוזיקה נעצרה.","assistant");return true;}}
  if("SETTINGS".equals(code)){int idx=setting>=0?setting:settingForRequest(norm(q));if(idx>=0){try{startActivity(engine.settingIntent(idx));addMessage("פותח הגדרות: "+settingName(idx),"assistant");return true;}catch(Exception ignored){}}}
  return false;
 }

 int settingForRequest(String x){
  if(hasAny(x,"wifi","wi fi","רשת אלחוטית","ויפי","וויפיי","וייפיי","וויפי","אלחוטי"))return 0;
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
  x=x.replaceAll("^(תפתח|פתח|לפתוח|פתיחה|open|launch|start|run)\\s+","");
  x=x.replaceAll("\\b(לי|את|האפליקציה|אפליקציה|אפליקציית|שלי)\\b"," ").replaceAll("\\s+"," ").trim();
  String[] filler={"פתח","תפתח","לפתוח","פתיחה","לי","את","בבקשה","open","launch","start","run","please","app"};
  for(String w:filler)x=x.replaceAll("(?iu)(^| )"+java.util.regex.Pattern.quote(norm(w))+"(?= |$)"," ");
  return x.trim();
 }

 boolean openThing(String q){
  String target=targetOf(q);
  if(!installedAppsLoaded){
   addMessage(english?"Loading the installed apps list...":"טוען את רשימת האפליקציות המותקנות...","assistant");
   ensureInstalledApps(()->openThing(q));
   return true;
  }
  engine.loadApps(this);
  String wanted=engine.canonical(target);
  PackageManager pm=getPackageManager();

  // Personal command/alias always wins.
  String customPkg=findCustomCommandPackage(q);
  if(customPkg!=null&&launchPackage(customPkg,q))return true;
  String userPkg=findUserAliasPackage(target);
  if(userPkg!=null && launchPackage(userPkg,target))return true;

  // Deterministic mappings for common apps/Android components.
  if(launchKnownApp(target))return true;

  AppRow best=null;int bestScore=0;
  for(AppRow row:installedApps){
   int s=appSpecialScore(wanted,row.packageName);
   if(s==0 && wanted.equals(engine.canonical(row.label)))s=120;
   String hay=row.label+" "+row.packageName.replace('.',' ');
   s=Math.max(s,engine.score(target,hay));
   s=Math.max(s,engine.score(wanted,hay));
   if(s>bestScore){bestScore=s;best=row;}
  }

  if(best!=null && bestScore>=18){
   if(launchPackage(best.packageName,target))return true;
  }

  ResolveInfo semantic=semanticApp(pm,target);
  if(semantic!=null){
   try{
    Intent i=new Intent();
    i.setComponent(new ComponentName(semantic.activityInfo.packageName,semantic.activityInfo.name));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(i);
    addMessage((english?"Opening ":"פותח ")+target,"assistant");return true;
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

 boolean launchBestInstalled(String target){
  String q=OfflineEngine.normalize(target);
  if(q.isEmpty())return false;
  if(!installedAppsLoaded)loadInstalledApps();
  AppRow best=null;int bestScore=0;
  for(AppRow row:installedApps){
   int score=engine.score(q,row.label+" "+row.packageName.replace('.',' '));
   String aliases=userAliases(row.packageName);
   if(!aliases.isEmpty())score=Math.max(score,engine.score(q,aliases)+25);
   if(score>bestScore){bestScore=score;best=row;}
  }
  return best!=null&&bestScore>=35&&launchPackage(best.packageName,target);
 }

 boolean launchPackage(String pkg,String spoken){
  if(pkg==null||pkg.trim().isEmpty())return false;
  try{
   PackageManager pm=getPackageManager();
   Intent i=pm.getLaunchIntentForPackage(pkg);
   if(i==null&&installedAppsLoaded){
    for(AppRow row:installedApps){
     if(pkg.equals(row.packageName)&&row.activityName!=null&&!row.activityName.isEmpty()){
      i=new Intent(Intent.ACTION_MAIN);
      i.addCategory(Intent.CATEGORY_LAUNCHER);
      i.setComponent(new ComponentName(pkg,row.activityName));
      break;
     }
    }
   }
   if(i==null)return false;
   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   startActivity(i);
   addMessage((english?"Opening ":"פותח ")+spoken,"assistant");
   return true;
  }catch(Exception e){return false;}
 }

 boolean launchKnownApp(String target){
  String w=OfflineEngine.normalize(engine.canonical(target));
  PackageManager pm=getPackageManager();

  String userPkg=findUserAliasPackage(target);
  if(userPkg!=null && launchPackage(userPkg,target))return true;

  String[] packages=null;
  if(w.equals("play store"))packages=new String[]{"com.android.vending"};
  else if(w.equals("google drive"))packages=new String[]{"com.google.android.apps.docs"};
  else if(w.equals("chrome")||w.equals("google chrome"))packages=new String[]{"com.android.chrome"};
  else if(w.equals("gmail"))packages=new String[]{"com.google.android.gm"};
  else if(w.equals("google maps")||w.equals("maps"))packages=new String[]{"com.google.android.apps.maps"};
  else if(w.equals("youtube"))packages=new String[]{"com.google.android.youtube"};
  else if(w.equals("google photos"))packages=new String[]{"com.google.android.apps.photos"};
  else if(w.equals("google"))packages=new String[]{"com.google.android.googlequicksearchbox"};
  else if(w.equals("calendar"))packages=new String[]{"com.google.android.calendar"};
  else if(w.equals("google keep"))packages=new String[]{"com.google.android.keep"};
  else if(w.equals("google translate"))packages=new String[]{"com.google.android.apps.translate"};
  else if(w.equals("whatsapp"))packages=new String[]{"com.whatsapp"};
  else if(w.equals("telegram"))packages=new String[]{"org.telegram.messenger"};
  else if(w.equals("spotify"))packages=new String[]{"com.spotify.music"};
  else if(w.equals("discord"))packages=new String[]{"com.discord"};
  else if(w.equals("facebook"))packages=new String[]{"com.facebook.katana"};
  else if(w.equals("instagram"))packages=new String[]{"com.instagram.android"};

  if(packages!=null){
   for(String pkg:packages)if(launchPackage(pkg,target))return true;
  }

  String category=null;
  if(w.equals("calculator"))category=Intent.CATEGORY_APP_CALCULATOR;
  else if(w.equals("gallery"))category=Intent.CATEGORY_APP_GALLERY;
  else if(w.equals("music")||w.equals("media player"))category=Intent.CATEGORY_APP_MUSIC;
  else if(w.equals("browser"))category=Intent.CATEGORY_APP_BROWSER;
  else if(w.equals("calendar"))category=Intent.CATEGORY_APP_CALENDAR;
  else if(w.equals("contacts"))category=Intent.CATEGORY_APP_CONTACTS;
  else if(w.equals("email"))category=Intent.CATEGORY_APP_EMAIL;
  else if(w.equals("files"))category=Intent.CATEGORY_APP_FILES;
  else if(w.equals("maps"))category=Intent.CATEGORY_APP_MAPS;
  else if(w.equals("play store"))category=Intent.CATEGORY_APP_MARKET;
  else if(w.equals("messages"))category=Intent.CATEGORY_APP_MESSAGING;
  else if(w.equals("weather"))category=Intent.CATEGORY_APP_WEATHER;

  if(category!=null){
   try{
    if(category.equals(Intent.CATEGORY_APP_FILES) && Build.VERSION.SDK_INT<29)return false;
    Intent i=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,category);
    List<ResolveInfo> list=pm.queryIntentActivities(i,PackageManager.MATCH_ALL);
    if(!list.isEmpty()){
     ResolveInfo ri=list.get(0);
     Intent launch=new Intent();
     launch.setComponent(new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name));
     launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     startActivity(launch);
     addMessage((english?"Opening ":"פותח ")+target,"assistant");return true;
    }
   }catch(Exception ignored){}
  }
  if(launchBestInstalled(target))return true;
  return false;
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