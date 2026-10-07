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
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import org.json.*;
import android.view.KeyEvent;
import android.accessibilityservice.AccessibilityService;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 ExecutorService responseExecutor=Executors.newSingleThreadExecutor(r->{
  Thread t=new Thread(r,"response-worker");
  t.setPriority(Thread.NORM_PRIORITY);
  return t;
 });
 volatile int responseRequestId=0;
 volatile Map<String,AppRow> installedExactIndex=Collections.emptyMap();
 volatile Map<String,ArrayList<AppRow>> installedTokenIndex=Collections.emptyMap();

 enum Mode{CHAT,APP,FILE}
 Mode mode=Mode.CHAT;
 LinearLayout sidebar,sidebarList,welcomePanel,composer;
 FrameLayout mainFrame;
 TextView modeLabel;
 String currentChatId;
 SharedPreferences historyPrefs;
 LinkedHashMap<String,ChatSession> chatSessions=new LinkedHashMap<>();
 final int BG=Color.rgb(250,248,242),PANEL=Color.rgb(246,243,236),TEXT=Color.rgb(37,35,31),MUTED=Color.rgb(119,113,103);
 final int BUBBLE=Color.rgb(255,254,250),USER_BUBBLE=Color.rgb(237,232,220),BORDER=Color.rgb(224,219,208),DARK=Color.rgb(30,29,27);
 static class ChatMessage{String text,who;ChatMessage(String t,String w){text=t;who=w;}}
 static class ChatSession{String id,title;ArrayList<ChatMessage> messages=new ArrayList<>();ChatSession(String i,String t){id=i;title=t;}}

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(BG);
  getWindow().setNavigationBarColor(BG);
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  engine=new OfflineEngine(this);
  aliasPrefs=getSharedPreferences("app_aliases",MODE_PRIVATE);
  historyPrefs=getSharedPreferences("chat_history",MODE_PRIVATE);
  loadHistory();
  buildChatUi();
  if(currentChatId==null||!chatSessions.containsKey(currentChatId))newChat();
  else renderCurrentSession();
  refreshSidebar();
  new Thread(()->{
   try{
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
    engine.loadChat(this);
   }catch(Exception ignored){}
  },"response-warmup").start();
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
  root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);root.setBackgroundColor(BG);

  LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);main.setBackgroundColor(BG);main.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

  LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(18),dp(10),dp(18),dp(8));
  ImageView mini=new ImageView(this);mini.setImageResource(R.drawable.ic_avraham);head.addView(mini,new LinearLayout.LayoutParams(dp(38),dp(38)));
  LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(10),0,0,0);
  TextView title=label("אברהם העברי",18,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);ht.addView(title);
  status=label("אופליין • מוכן",11,MUTED);ht.addView(status);
  head.addView(ht,new LinearLayout.LayoutParams(0,-2,1));main.addView(head,new LinearLayout.LayoutParams(-1,dp(62)));

  mainFrame=new FrameLayout(this);main.addView(mainFrame,new LinearLayout.LayoutParams(-1,0,1));

  welcomePanel=new LinearLayout(this);welcomePanel.setOrientation(LinearLayout.VERTICAL);welcomePanel.setGravity(Gravity.CENTER_HORIZONTAL);
  welcomePanel.setPadding(dp(24),0,dp(24),dp(8));
  ImageView welcomeIcon=new ImageView(this);welcomeIcon.setImageResource(R.drawable.ic_avraham);
  welcomePanel.addView(welcomeIcon,new LinearLayout.LayoutParams(dp(112),dp(112)));
  TextView welcomeTitle=label("אברהם העברי",25,TEXT);welcomeTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);welcomeTitle.setGravity(Gravity.CENTER);
  welcomePanel.addView(welcomeTitle,new LinearLayout.LayoutParams(-1,dp(42)));
  TextView welcomeSub=label("העוזר המקומי שלך",14,MUTED);welcomeSub.setGravity(Gravity.CENTER);
  welcomePanel.addView(welcomeSub,new LinearLayout.LayoutParams(-1,dp(30)));

  composer=buildComposer();
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(70));cp.setMargins(0,dp(22),0,0);welcomePanel.addView(composer,cp);
  FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-1,-2);wp.gravity=Gravity.CENTER;mainFrame.addView(welcomePanel,wp);

  chatScroll=new ScrollView(this);chatScroll.setFillViewport(true);chatScroll.setVerticalScrollBarEnabled(false);
  chatList=new LinearLayout(this);chatList.setOrientation(LinearLayout.VERTICAL);chatList.setPadding(dp(12),dp(14),dp(12),dp(96));
  chatScroll.addView(chatList,new ScrollView.LayoutParams(-1,-2));
  FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-1);sp.gravity=Gravity.FILL;mainFrame.addView(chatScroll,sp);chatScroll.setVisibility(View.GONE);

  root.addView(main,new LinearLayout.LayoutParams(0,-1,1));

  sidebar=new LinearLayout(this);sidebar.setOrientation(LinearLayout.VERTICAL);sidebar.setBackgroundColor(PANEL);sidebar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  LinearLayout sbHead=new LinearLayout(this);sbHead.setGravity(Gravity.CENTER_VERTICAL);sbHead.setPadding(dp(10),dp(10),dp(10),dp(10));

  Button sbPlus=softButton("+");sbPlus.setTextSize(22);sbPlus.setTextColor(Color.WHITE);sbPlus.setBackground(shape(DARK,14,0));sbPlus.setOnClickListener(v->showModeMenu(v));
  sbHead.addView(sbPlus,new LinearLayout.LayoutParams(dp(44),dp(44)));
  Button gear=softButton("⚙");gear.setTextSize(18);gear.setPadding(0,0,0,0);gear.setOnClickListener(v->showAppManager());
  LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(44),dp(44));gp.setMargins(dp(6),0,0,0);sbHead.addView(gear,gp);
  TextView sbTitle=label("השיחות שלי",16,TEXT);sbTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);sbTitle.setGravity(Gravity.CENTER);sbHead.addView(sbTitle,new LinearLayout.LayoutParams(0,dp(44),1));
  TextView collapse=label("‹",25,MUTED);collapse.setGravity(Gravity.CENTER);sbHead.addView(collapse,new LinearLayout.LayoutParams(dp(30),dp(44)));
  sidebar.addView(sbHead);

  View divider=new View(this);divider.setBackgroundColor(BORDER);sidebar.addView(divider,new LinearLayout.LayoutParams(-1,1));

  Button newChatBtn=softButton("שיחה חדשה");newChatBtn.setTypeface(Typeface.DEFAULT,Typeface.BOLD);newChatBtn.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);newChatBtn.setOnClickListener(v->newChat());
  LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(48));np.setMargins(dp(10),dp(10),dp(10),dp(6));sidebar.addView(newChatBtn,np);

  sidebarList=new LinearLayout(this);sidebarList.setOrientation(LinearLayout.VERTICAL);sidebarList.setPadding(dp(10),0,dp(10),0);
  ScrollView hs=new ScrollView(this);hs.setVerticalScrollBarEnabled(false);hs.addView(sidebarList,new ScrollView.LayoutParams(-1,-2));
  sidebar.addView(hs,new LinearLayout.LayoutParams(-1,0,1));

  root.addView(sidebar,new LinearLayout.LayoutParams(dp(286),-1));
  setContentView(root);
  getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
 }

 LinearLayout buildComposer(){
  LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.HORIZONTAL);wrap.setGravity(Gravity.CENTER_VERTICAL);
  wrap.setPadding(dp(8),dp(6),dp(8),dp(6));wrap.setBackground(shape(BUBBLE,24,1));

  Button send=softButton("➤");send.setTextSize(20);send.setTextColor(Color.WHITE);send.setPadding(0,0,0,2);send.setBackground(shape(DARK,17,0));send.setOnClickListener(v->sendCurrent());
  wrap.addView(send,new LinearLayout.LayoutParams(dp(48),dp(56)));

  input=new EditText(this);input.setTextSize(16);input.setTextColor(TEXT);input.setHintTextColor(Color.rgb(155,149,139));
  input.setHint("כתוב הודעה...");input.setSingleLine(false);input.setMaxLines(3);input.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);input.setPadding(dp(10),dp(8),dp(10),dp(8));input.setBackgroundColor(Color.TRANSPARENT);
  wrap.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));

  modeLabel=label("צ׳אט",12,MUTED);modeLabel.setGravity(Gravity.CENTER);wrap.addView(modeLabel,new LinearLayout.LayoutParams(dp(52),dp(40)));

  Button plus=softButton("+");plus.setTextSize(22);plus.setPadding(0,0,0,2);plus.setBackground(shape(Color.TRANSPARENT,18,0));plus.setOnClickListener(v->showModeMenu(v));
  wrap.addView(plus,new LinearLayout.LayoutParams(dp(44),dp(56)));
  return wrap;
 }

 int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

 void showModeMenu(View anchor){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),dp(10),dp(10),dp(10));box.setBackground(shape(PANEL,18,1));
  PopupWindow pw=new PopupWindow(box,dp(250),-2,true);pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));pw.setElevation(dp(8));
  addModeItem(box,pw,"חיפוש קובץ","חפש קובץ בתיקייה שנבחרה ופתח אותו",Mode.FILE);
  addModeItem(box,pw,"פתיחת אפליקציה","חפש אפליקציה מותקנת ופתח אותה",Mode.APP);
  addModeItem(box,pw,"צ׳אט","נסה תשובה; אם אין התאמה, נסה אפליקציה ואז קבצים",Mode.CHAT);
  pw.showAsDropDown(anchor,-dp(190),-dp(220));
 }

 void addModeItem(LinearLayout box,PopupWindow pw,String titleText,String sub,Mode m){
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(14),dp(8),dp(14),dp(8));row.setBackground(shape(BUBBLE,14,1));
  TextView a=label(titleText,15,TEXT);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);a.setGravity(Gravity.RIGHT);row.addView(a);
  TextView b=label(sub,11,MUTED);b.setGravity(Gravity.RIGHT);row.addView(b);
  row.setOnClickListener(v->{setMode(m);pw.dismiss();});
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(62));rp.setMargins(0,dp(4),0,dp(4));box.addView(row,rp);
 }

 void setMode(Mode m){
  mode=m;
  String name=mode==Mode.CHAT?"צ׳אט":mode==Mode.APP?"אפליקציה":"קובץ";
  modeLabel.setText(name);
  input.setHint(mode==Mode.CHAT?"כתוב הודעה...":mode==Mode.APP?"שם האפליקציה לפתיחה...":"שם הקובץ לחיפוש...");
 }

 void addSuggestion(LinearLayout box,String text){
  Button b=softButton(text);b.setTextSize(12);b.setOnClickListener(v->{input.setText(text);sendCurrent();});
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,44,1);lp.setMargins(3,0,3,0);box.addView(b,lp);
 }

 TextView bubble(String text,boolean user){
  TextView t=label(text,16,TEXT);t.setLineSpacing(0,1.12f);t.setPadding(16,12,16,12);t.setBackground(shape(user?USER_BUBBLE:BUBBLE,22,1));
  if(user)t.setTextColor(TEXT);
  return t;
 }

 void addMessage(String text,String who){
  if(Looper.myLooper()!=Looper.getMainLooper()){runOnUiThread(()->addMessage(text,who));return;}
  boolean user="user".equals(who);
  if(user&&!isActiveChat())setChatActive(true);
  renderMessage(text,who,true);
  ChatSession cs=chatSessions.get(currentChatId);
  if(cs!=null){
   cs.messages.add(new ChatMessage(text,who));
   if(user&&("שיחה חדשה".equals(cs.title)||cs.title.trim().isEmpty())){
    String t=text.replaceAll("\\s+"," ").trim();cs.title=t.length()>28?t.substring(0,28)+"…":t;
   }
   saveHistory();refreshSidebar();
  }
 }

 void renderMessage(String text,String who,boolean actions){
  boolean user="user".equals(who);
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setGravity(user?Gravity.RIGHT:Gravity.LEFT);row.setPadding(dp(8),dp(4),dp(8),dp(4));
  TextView b=bubble(text,user);
  LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(user?dp(52):dp(8),dp(1),user?dp(8):dp(52),dp(1));row.addView(b,bp);
  if(actions){
   LinearLayout tools=new LinearLayout(this);tools.setGravity(user?Gravity.RIGHT:Gravity.LEFT);
   Button copy=miniAction("העתק");copy.setOnClickListener(v->{
    android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    cm.setPrimaryClip(android.content.ClipData.newPlainText("message",text));Toast.makeText(this,"הועתק",Toast.LENGTH_SHORT).show();
   });
   Button again=miniAction("שוב");again.setOnClickListener(v->replayMessage(text,user));
   tools.addView(copy);tools.addView(again);
   LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,dp(32));tp.setMargins(user?dp(52):dp(8),0,user?dp(8):dp(52),0);row.addView(tools,tp);
  }
  chatList.addView(row,new LinearLayout.LayoutParams(-1,-2));chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
 }

 Button miniAction(String s){Button b=softButton(s);b.setTextSize(11);b.setMinHeight(dp(28));b.setPadding(dp(12),0,dp(12),0);b.setBackground(shape(Color.TRANSPARENT,12,0));return b;}
 boolean isActiveChat(){return welcomePanel!=null&&welcomePanel.getVisibility()!=View.VISIBLE;}

 void replayMessage(String text,boolean wasUser){
  String q=wasUser?text:lastUserMessage();if(q==null||q.trim().isEmpty())return;
  input.setText(q);input.setSelection(input.length());sendCurrent();
 }

 String lastUserMessage(){
  ChatSession cs=chatSessions.get(currentChatId);if(cs==null)return null;
  for(int i=cs.messages.size()-1;i>=0;i--)if("user".equals(cs.messages.get(i).who))return cs.messages.get(i).text;
  return null;
 }

 void setChatActive(boolean active){
  if(active){welcomePanel.setVisibility(View.GONE);chatScroll.setVisibility(View.VISIBLE);moveComposerToMain();}
  else{chatScroll.setVisibility(View.GONE);welcomePanel.setVisibility(View.VISIBLE);moveComposerToWelcome();}
 }

 void moveComposerToWelcome(){
  if(composer==null||welcomePanel==null)return;
  if(composer.getParent()!=null)((ViewGroup)composer.getParent()).removeView(composer);
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(70));cp.setMargins(0,dp(22),0,0);welcomePanel.addView(composer,cp);
 }

 void moveComposerToMain(){
  if(composer==null||root==null)return;
  if(composer.getParent()!=null)((ViewGroup)composer.getParent()).removeView(composer);
  LinearLayout main=(LinearLayout)root.getChildAt(0);
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(78));cp.setMargins(dp(16),0,dp(16),dp(10));main.addView(composer,cp);
 }

 void sendCurrent(){
  String q=input.getText().toString().trim();if(q.isEmpty())return;
  input.setText("");addMessage(q,"user");
  if(mode==Mode.APP){openThing(q);return;}
  if(mode==Mode.FILE){searchFiles(q,false);return;}
  process(q);
 }

 static class AppRow{
  String label,packageName,activityName;
  boolean system,launchable;
  AppRow(String l,String p,String a,boolean s,boolean z){label=l;packageName=p;activityName=a;system=s;launchable=z;}
 }

 void loadHistory(){
  chatSessions.clear();
  String raw=historyPrefs==null?"[]":historyPrefs.getString("sessions","[]");
  try{
   JSONArray arr=new JSONArray(raw);
   for(int i=0;i<arr.length();i++){
    JSONObject o=arr.getJSONObject(i);ChatSession cs=new ChatSession(o.optString("id",""),o.optString("title","שיחה חדשה"));
    JSONArray ms=o.optJSONArray("messages");
    if(ms!=null)for(int j=0;j<ms.length();j++){JSONObject m=ms.getJSONObject(j);cs.messages.add(new ChatMessage(m.optString("text",""),m.optString("who","assistant")));}
    if(!cs.id.isEmpty())chatSessions.put(cs.id,cs);
   }
   if(!chatSessions.isEmpty()){ArrayList<ChatSession> list=new ArrayList<>(chatSessions.values());currentChatId=list.get(list.size()-1).id;}
  }catch(Exception ignored){}
 }

 void saveHistory(){
  if(historyPrefs==null)return;
  try{
   JSONArray arr=new JSONArray();int skip=Math.max(0,chatSessions.size()-40),i=0;
   for(ChatSession cs:chatSessions.values()){
    if(i++<skip)continue;
    JSONObject o=new JSONObject();o.put("id",cs.id);o.put("title",cs.title);JSONArray ms=new JSONArray();
    int start=Math.max(0,cs.messages.size()-250);
    for(int j=start;j<cs.messages.size();j++){ChatMessage m=cs.messages.get(j);JSONObject x=new JSONObject();x.put("text",m.text);x.put("who",m.who);ms.put(x);}
    o.put("messages",ms);arr.put(o);
   }
   historyPrefs.edit().putString("sessions",arr.toString()).apply();
  }catch(Exception ignored){}
 }

 void newChat(){
  currentChatId=Long.toString(System.currentTimeMillis());
  chatSessions.put(currentChatId,new ChatSession(currentChatId,"שיחה חדשה"));
  chatList.removeAllViews();setChatActive(false);input.setText("");setMode(Mode.CHAT);saveHistory();refreshSidebar();
 }

 void renderCurrentSession(){
  chatList.removeAllViews();ChatSession cs=chatSessions.get(currentChatId);
  if(cs==null||cs.messages.isEmpty()){setChatActive(false);return;}
  setChatActive(true);for(ChatMessage m:cs.messages)renderMessage(m.text,m.who,true);
 }

 void openSession(String id){if(!chatSessions.containsKey(id))return;currentChatId=id;renderCurrentSession();refreshSidebar();}
 void deleteSession(String id){
  chatSessions.remove(id);
  if(chatSessions.isEmpty()){newChat();return;}
  ArrayList<ChatSession> list=new ArrayList<>(chatSessions.values());currentChatId=list.get(list.size()-1).id;renderCurrentSession();saveHistory();refreshSidebar();
 }

 void refreshSidebar(){
  if(sidebarList==null)return;sidebarList.removeAllViews();
  ArrayList<ChatSession> list=new ArrayList<>(chatSessions.values());Collections.reverse(list);
  for(ChatSession cs:list){
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(3),dp(5),dp(3));
   row.setBackground(shape(cs.id.equals(currentChatId)?Color.rgb(231,227,218):Color.TRANSPARENT,14,0));
   TextView name=label(cs.title==null||cs.title.isEmpty()?"שיחה חדשה":cs.title,14,TEXT);name.setSingleLine(true);name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(name,new LinearLayout.LayoutParams(0,dp(44),1));
   Button del=softButton("⌫");del.setTextSize(13);del.setPadding(0,0,0,0);del.setBackground(shape(Color.TRANSPARENT,12,0));del.setOnClickListener(v->deleteSession(cs.id));row.addView(del,new LinearLayout.LayoutParams(dp(40),dp(42)));
   row.setOnClickListener(v->openSession(cs.id));sidebarList.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));
  }
 }

 void searchFiles(String q,boolean fromChat){
  final String query=OfflineEngine.normalize(q);if(query.isEmpty())return;
  String tree=getPreferences(MODE_PRIVATE).getString("tree","");
  if(tree.isEmpty()){
   if(fromChat)addMessage("לא מצאתי תשובה מתאימה, ולא נבחרה עדיין תיקייה לחיפוש קבצים.","assistant");
   else{addMessage("בחר תיקייה לחיפוש קבצים, ואז אחפש בה לפי שם.","assistant");pickFolder();}
   return;
  }
  addMessage("מחפש קובץ…","assistant");
  new Thread(()->{
   Uri treeUri;try{treeUri=Uri.parse(tree);}catch(Exception e){runOnUiThread(()->addMessage("לא הצלחתי לקרוא את תיקיית הקבצים השמורה.","assistant"));return;}
   Uri found=null;String mime="*/*";int checked=0;ArrayDeque<String> queue=new ArrayDeque<>();
   try{
    String rootId=DocumentsContract.getTreeDocumentId(treeUri);queue.add(rootId+"\t0");
    while(!queue.isEmpty()&&checked<2500&&found==null){
     String[] item=queue.removeFirst().split("\\t",-1);String parentId=item[0];int depth=Integer.parseInt(item[1]);
     Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,parentId);
     try(android.database.Cursor cur=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
      if(cur==null)continue;
      while(cur.moveToNext()&&checked<2500){
       checked++;String id=cur.getString(0),name=cur.getString(1)==null?"":cur.getString(1),mt=cur.getString(2);String nn=OfflineEngine.normalize(name);
       if(!DocumentsContract.Document.MIME_TYPE_DIR.equals(mt)&&(nn.equals(query)||nn.contains(query))){found=DocumentsContract.buildDocumentUriUsingTree(treeUri,id);mime=(mt==null||mt.isEmpty())?"*/*":mt;break;}
       if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mt)&&depth<8)queue.add(id+"\t"+(depth+1));
      }
     }
    }
   }catch(Exception ignored){}
   Uri result=found;String resultMime=mime;
   runOnUiThread(()->{
    if(result==null){addMessage("לא מצאתי קובץ מתאים בתיקייה שבחרת.","assistant");return;}
    try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(result,resultMime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);addMessage("מצאתי קובץ ופתחתי אותו.","assistant");}
    catch(Exception e){addMessage("מצאתי את הקובץ, אבל Android לא מצא אפליקציה מתאימה לפתיחה שלו.","assistant");}
   });
  },"file-search-worker").start();
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

   HashMap<String,AppRow> exact=new HashMap<>();
   HashMap<String,ArrayList<AppRow>> tokens=new HashMap<>();
   for(AppRow row:out){
    if(!row.launchable)continue;
    String nl=OfflineEngine.normalize(row.label);
    String np=OfflineEngine.normalize(row.packageName);
    addInstalledExact(exact,nl,row);
    addInstalledExact(exact,np,row);
    String canon=OfflineEngine.normalize(engine.canonical(nl));
    addInstalledExact(exact,canon,row);
    for(String tok:(nl+" "+np).split("\\s+")){
     if(tok.length()<2)continue;
     ArrayList<AppRow> list=tokens.get(tok);
     if(list==null){list=new ArrayList<>();tokens.put(tok,list);}
     if(list.size()<24&&!list.contains(row))list.add(row);
    }
   }
   installedExactIndex=exact;
   installedTokenIndex=tokens;
   installedApps=out;
   installedAppsLoaded=true;
  }catch(Exception ignored){}
 }

 void addInstalledExact(HashMap<String,AppRow> index,String key,AppRow row){
  if(key==null||key.isEmpty())return;
  if(!index.containsKey(key))index.put(key,row);
 }

 AppRow findInstalledMatch(String target){
  String q=OfflineEngine.normalize(target);
  if(q.isEmpty())return null;

  AppRow exact=installedExactIndex.get(q);
  if(exact!=null)return exact;

  String canonical=OfflineEngine.normalize(engine.canonical(q));
  if(!canonical.equals(q)){
   exact=installedExactIndex.get(canonical);
   if(exact!=null)return exact;
  }

  LinkedHashSet<AppRow> candidates=new LinkedHashSet<>();
  for(String tok:q.split("\\s+")){
   if(tok.length()<2)continue;
   ArrayList<AppRow> list=installedTokenIndex.get(tok);
   if(list!=null)for(AppRow row:list){
    candidates.add(row);
    if(candidates.size()>=32)break;
   }
   if(candidates.size()>=32)break;
  }

  AppRow best=null;int bestScore=0;
  for(AppRow row:candidates){
   String label=OfflineEngine.normalize(row.label);
   String pkg=OfflineEngine.normalize(row.packageName);
   int s=0;
   if(q.equals(label)||q.equals(pkg))s=120;
   else if(canonical.equals(label)||canonical.equals(pkg))s=115;
   else if(label.startsWith(q)||pkg.startsWith(q))s=90;
   else if(q.length()>=3&&(label.contains(q)||pkg.contains(q)))s=75;
   else {
    int hits=0;
    for(String tok:q.split("\\s+")){
     if(tok.length()<2)continue;
     if(label.contains(tok)||pkg.contains(tok))hits++;
    }
    s=hits*22;
   }
   if(s>bestScore){bestScore=s;best=row;}
  }
  return bestScore>=22?best:null;
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
    if(q.equals(x)||engine.score(q,x)>=55)return e.getKey();
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
   if(!x.isEmpty()){
    set.add(x);
    String y=cleanUserCommand(x);if(!y.isEmpty())set.add(y);
    removeCommandFromOtherApps(x,row.packageName);
    if(!y.isEmpty())removeCommandFromOtherApps(y,row.packageName);
   }
  }
  aliasPrefs.edit().putString(row.packageName,String.join("|",set)).apply();
 }
 void removeCommandFromOtherApps(String command,String keepPackage){
  if(aliasPrefs==null)return;
  String q=OfflineEngine.normalize(command);
  if(q.isEmpty())return;
  SharedPreferences.Editor editor=aliasPrefs.edit();
  boolean changed=false;
  for(Map.Entry<String,?> e:aliasPrefs.getAll().entrySet()){
   if(e.getKey().equals(keepPackage)||!(e.getValue() instanceof String))continue;
   LinkedHashSet<String> keep=new LinkedHashSet<>();
   boolean packageChanged=false;
   for(String a:((String)e.getValue()).split("\\|")){
    String x=OfflineEngine.normalize(a);
    if(!x.equals(q))keep.add(x);
    else packageChanged=true;
   }
   if(packageChanged){changed=true;editor.putString(e.getKey(),String.join("|",keep));}
  }
  if(changed)editor.apply();
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

  EditText search=new EditText(this);search.setHint("חפש אפליקציה או חבילה...");search.setSingleLine(true);
  search.setTextSize(15);search.setPadding(16,8,16,8);search.setBackground(shape(Color.WHITE,22,1));
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,50);sp.setMargins(0,12,0,10);box.addView(search,sp);

  ListView list=new ListView(this);list.setDividerHeight(1);
  AppAdapter adapter=new AppAdapter(installedApps);list.setAdapter(adapter);

  LinearLayout actions=new LinearLayout(this);
  Button roles=softButton("בדיקת Android");roles.setOnClickListener(v->showAndroidRoles());
  Button refresh=softButton("רענן");refresh.setOnClickListener(v->{refresh.setEnabled(false);new Thread(()->{loadInstalledApps();runOnUiThread(()->{count.setText("נטענו "+installedApps.size()+" אפליקציות מהמכשיר");adapter.reload(installedApps);refresh.setEnabled(true);});},"installed-app-refresh").start();});
  actions.addView(roles,new LinearLayout.LayoutParams(0,44,1));
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,44,1);rp.setMargins(8,0,0,0);actions.addView(refresh,rp);
  box.addView(actions);
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
   void reload(ArrayList<AppRow> a){all=new ArrayList<>(a);filter("");}
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
 boolean hasWordOrPhrase(String q,String term){
  String n=norm(q),t=norm(term);if(n.isEmpty()||t.isEmpty())return false;
  return n.equals(t)||n.startsWith(t+" ")||n.endsWith(" "+t)||n.contains(" "+t+" ");
 }
 boolean hasAnyWordOrPhrase(String q,String...terms){for(String t:terms)if(hasWordOrPhrase(q,t))return true;return false;}
 boolean openRequest(String q){return hasAny(q,"פתח","תפתח","לפתוח","פתיחה","open","launch","start","run");}
 boolean startsCommand(String q,String term){
  String n=norm(q),t=norm(term);if(n.equals(t)||n.startsWith(t+" "))return true;
  return n.startsWith("בבקשה "+t)||n.startsWith("please "+t);
 }
 boolean anyStartsCommand(String q,String...terms){for(String t:terms)if(startsCommand(q,t))return true;return false;}
 boolean actionRequest(String q){
  String x=norm(q);
  if(anyStartsCommand(x,"תגביה","תגביהה","תנמיך","השתק","השתקה","נגן","השהה","עצור","חזור אחורה","חזור הביתה","אחורה","אפליקציות אחרונות","אחרונות","פתח התראות","התראות","הגדרות מהירות","צלם מסך","צילום מסך","נעל מסך","נעילת מסך","volume","mute","play","pause","home","back","notifications","quick settings","screenshot","lock screen"))return true;
  return (startsCommand(x,"הבא")||startsCommand(x,"קודם")||startsCommand(x,"next")||startsCommand(x,"previous"))&&hasAnyWordOrPhrase(x,"שיר","מוזיקה","track","song");
 }
 boolean likelyActionCommand(String q){
  String x=norm(q);
  return anyStartsCommand(x,
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
  if(!hasAnyWordOrPhrase(x,"wifi","wi-fi","רשת אלחוטית","ויפי","וויפיי","וייפיי","bluetooth","בלוטוס","בלוטות","מצב טיסה","airplane","נקודה חמה","hotspot","vpn","dns","תצוגה","display","notification settings","אחסון","storage","הרשאות","permissions","מיקום","location","מקלדת","keyboard","שפה","language","תאריך","date","שעה","time","nfc","שידור מסך","cast","sound","שמע"))return false;
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
  try{
  // Tier 0: canned/common replies. These must never touch any catalog.
  OfflineEngine.Resp instant=engine.quickResponse(q);
  if(instant!=null){
   addMessage(english?instant.en:instant.he,"assistant");
   return;
  }

  // Tier 1: deterministic Android commands before any offline catalogs.
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
  }catch(Exception ex){
   android.util.Log.e("Avraham","process failed",ex);
   addMessage(english?"I hit a local processing error and recovered.":"אירעה שגיאת עיבוד מקומית והאפליקציה התאוששה.","assistant");
  }
 }

 boolean appNameOnlyRequest(String q){
  String x=norm(q);
  if(x.isEmpty()||tokenCount(x)>6)return false;

  if(hasAnyWordOrPhrase(x,
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

  if(findCustomCommandPackage(x)!=null||findUserAliasPackage(x)!=null)return true;

  // Do not start PackageManager scans for normal short chat sentences.
  // App-name-only detection is intentionally conservative.
  if(tokenCount(x)>3 || hasAnyWordOrPhrase(x,
    "מה","איך","למה","מתי","איפה","מי","האם","אפשר","תוכל","תעזור",
    "תסביר","ספר","תן","תתן","אני","אתה","אנחנו","מהו","מהי","למה זה"))return false;

  // Do not touch the 4,000-entry catalog for ordinary text. First use the
  // tiny installed-app index; the big catalog is only a final fallback.
  if(!installedAppsLoaded){
   ensureInstalledApps(()->process(q));
   return true;
  }
  if(findInstalledMatch(x)!=null)return true;

  // A name known only to the offline catalog is NOT evidence that the app is
  // installed. Let openThing() report a clear "not found / may not be installed"
  // message instead of silently waiting or looping.
  if(engine.isKnownAppAlias(x))return true;

  if(!engine.appsLoaded){
   new Thread(()->{
    engine.loadApps(this);
    runOnUiThread(()->process(q));
   },"lazy-app-catalog-loader").start();
   return true;
  }
  return false;
 }

 int tokenCount(String x){return x.trim().isEmpty()?0:x.trim().split("\\s+").length;}

 int bestInstalledScore(String target){
  return findInstalledMatch(target)==null?0:100;
 }

 void chat(String q){
  OfflineEngine.Resp fast=engine.quickResponse(q);
  if(fast!=null){addMessage(english?fast.en:fast.he,"assistant");return;}
  final boolean responseEnglish=english;
  final int requestId=++responseRequestId;
  responseExecutor.execute(()->{
   try{
    if(!engine.chatLoaded){
     engine.loadChat(this);
    }
    // If a newer question arrived while loading, do not spend CPU matching
    // an obsolete question. The latest request will be processed instead.
    if(requestId!=responseRequestId)return;
    OfflineEngine.Resp r=engine.bestResponse(q,responseEnglish);
    runOnUiThread(()->{
     if(requestId!=responseRequestId)return;
     if(r==null){fallbackFromChat(q,responseEnglish);}else{
      addMessage(responseEnglish?r.en:r.he,"assistant");
     }
    });
   }catch(Exception ex){
    android.util.Log.e("Avraham","response worker failed",ex);
    runOnUiThread(()->{
     if(requestId==responseRequestId)addMessage(english?"Sorry, I had a local processing error.":"אירעה שגיאת עיבוד מקומית, אבל האפליקציה ממשיכה לפעול.","assistant");
    });
   }
  });
 }

 void fallbackFromChat(String q,boolean responseEnglish){
  if(!installedAppsLoaded){ensureInstalledApps(()->fallbackFromChat(q,responseEnglish));return;}
  String target=targetOf(q);AppRow app=findInstalledMatch(target);
  if(app!=null&&launchPackage(app.packageName,target))return;
  searchFiles(q,true);
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
  if(hasAnyWordOrPhrase(x,"מה השעה","מה השעה עכשיו","מה הזמן","what time is it","what's the time","current time")||x.equals("השעה")){
   String time=new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(new java.util.Date());
   addMessage(english?"The time is "+time+".":"השעה עכשיו "+time+".","assistant");
   return true;
  }

  // Android Home must work with both "מסך בית" and "מסך הבית".
  if(isHomeCommand(x)){
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
  return hasAnyWordOrPhrase(x,"מחשבון","calculator","שעון","clock","סייר קבצים","מנהל קבצים",
    "סייר הקבצים","קבצים","files","file manager","file explorer","גלריה","gallery",
    "מצלמה","camera","יומן","לוח שנה","calendar","טלפון","phone","חייגן","dialer",
    "הודעות","messages","אנשי קשר","contacts","דפדפן","browser","אינטרנט",
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
  return x.matches("^(פתח|תפתח|לפתוח|launch|open|start)(?: את)? (בית|מסך בית|מסך הבית|דף בית|דף הבית|home)$")
    || x.matches("^(חזור|תחזור|חזור ל|עבור|עבור ל|עבור אל|תעביר אותי ל|תעביר אותי אל) (בית|מסך בית|מסך הבית|דף בית|דף הבית|home)$")
    || x.equals("חזור הביתה") || x.equals("חזרה למסך הבית") || x.equals("עבור למסך הבית");
 }

 boolean openSetting(String action,String ok){
  try{Intent i=new Intent(action);if(i.resolveActivity(getPackageManager())==null)return false;startActivity(i);addMessage(ok,"assistant");return true;}catch(Exception e){return false;}
 }
 boolean quickToggleVerified(final boolean on,final String successMessage,final String failureAction,final String failureMessage,final String... labels){
  try{
   return ShortcutService.toggleQuickSetting(
    ()->addMessage(successMessage,"assistant"),
    ()->{if(!openSetting(failureAction,failureMessage))addMessage("לא הצלחתי לבצע את פעולת המערכת.","assistant");},
    labels);
  }catch(Exception e){return false;}
 }
 boolean systemToggle(String q){
  String x=norm(q);
  boolean on=hasAny(x,"תפעיל","הפעל","להפעיל","הדלק","שים","עבור למצב","תעביר אותי למצב","תעביר אותי למצב טיסה","שים במצב טיסה","turn on","enable");
  boolean off=hasAny(x,"תכבה","כבה","לכבות","כיבוי","turn off","disable");
  if(!on&&!off)return false;
  if(hasAny(x,"בלוטוס","בלוטות","bluetooth")){
   String ok=on?"הבלוטוס הופעל.":"הבלוטוס כובה.";
   if(quickToggleVerified(on,ok,android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,"לא הצלחתי לשנות את הבלוטוס. פתחתי את הגדרות הבלוטוס.","bluetooth","בלוטוס","בלוטות"))return true;
   openSetting(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,on?"לא הצלחתי להפעיל את הבלוטוס אוטומטית. פתחתי את הגדרות הבלוטוס.":"לא הצלחתי לכבות את הבלוטוס אוטומטית. פתחתי את הגדרות הבלוטוס.");
   return true;
  }
  if(hasAny(x,"ויפי","וויפיי","וייפיי","wifi","wi fi","רשת אלחוטית")){
   String ok=on?"ה־Wi‑Fi הופעל.":"ה־Wi‑Fi כובה.";
   if(quickToggleVerified(on,ok,android.provider.Settings.ACTION_WIFI_SETTINGS,"לא הצלחתי לשנות את ה־Wi‑Fi. פתחתי את הגדרות ה־Wi‑Fi.","wifi","wi fi","ויפי","וויפיי","וייפיי","internet","אינטרנט"))return true;
   return openSetting(android.provider.Settings.ACTION_WIFI_SETTINGS,"פתחתי את הגדרות ה־Wi‑Fi.");
  }
  if(hasAny(x,"מצב טיסה","airplane")){
   String ok=on?"מצב טיסה הופעל.":"מצב טיסה כובה.";
   if(quickToggleVerified(on,ok,android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS,"לא הצלחתי לשנות את מצב הטיסה. פתחתי את ההגדרות.","airplane","airplane mode","מצב טיסה"))return true;
   openSetting(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS,"פתחתי את הגדרות מצב הטיסה.");
   return true;
  }
  return false;
 }

 boolean media(int k){
  try{audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,k));audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,k));return true;}catch(Exception e){return false;}
 }
 boolean global(int action){return ShortcutService.doGlobal(action);}

 boolean direct(String q){
  String x=norm(q);
  if((x.equals("הגדרות")||x.equals("settings")||startsCommand(x,"פתח הגדרות")||startsCommand(x,"תפתח הגדרות")||startsCommand(x,"פתח את ההגדרות")||startsCommand(x,"תפתח את ההגדרות")||startsCommand(x,"היכנס להגדרות")||startsCommand(x,"open settings")) &&
     !hasAnyWordOrPhrase(x,"הגדרות התראות","התראות אפליקציה","notification settings","wifi","wi-fi","רשת אלחוטית","bluetooth","בלוטוס","מצב טיסה","airplane","נקודה חמה","hotspot","vpn","מסך","תצוגה","display","אחסון","storage","הרשאות","permissions","מיקום","location","מקלדת","keyboard","שפה","language","תאריך","date","שעה","time","nfc","שידור מסך","cast")){
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
   new Thread(()->{
    engine.loadCommands(this);
    runOnUiThread(()->runAction(q));
   },"commands-loader").start();
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
  // The 4,000-entry alias catalog is never loaded synchronously here.
  // The tiny installed-app index handles the normal case first.
  String wanted=engine.canonical(target);
  PackageManager pm=getPackageManager();

  // Personal command/alias always wins.
  String customPkg=findCustomCommandPackage(q);
  if(customPkg!=null&&launchPackage(customPkg,q))return true;
  String userPkg=findUserAliasPackage(target);
  if(userPkg!=null && launchPackage(userPkg,target))return true;

  // Deterministic mappings for common apps/Android components.
  if(launchKnownApp(target))return true;

  // Tiny installed-app search: exact lookup first, then at most 32
  // token candidates. Never score the entire installed-app list.
  AppRow best=findInstalledMatch(wanted);
  if(best!=null && launchPackage(best.packageName,target))return true;

  // If this is a catalog-known app but it is not installed, stop here. Do not
  // fall through to a generic Android category that could open a different app.
  if(engine.isKnownAppAlias(wanted) || engine.isKnownAppAlias(target)){
   addMessage(english?
     "I couldn't find that installed app. It may not be installed on this device.":
     "לא מצאתי את האפליקציה בין האפליקציות המותקנות. ייתכן שהיא לא מותקנת במכשיר.","assistant");
   return true;
  }

  // Only after the tiny installed-app search fails do we consult the 4,000-name
  // vocabulary catalog, and only once, in the background.
  if(!engine.appsLoaded){
   new Thread(()->{
    engine.loadApps(this);
    runOnUiThread(()->openThing(q));
   },"lazy-app-catalog-open").start();
   return true;
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
  AppRow best=findInstalledMatch(q);
  return best!=null&&launchPackage(best.packageName,target);
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

 @Override protected void onDestroy(){
  responseExecutor.shutdownNow();
  super.onDestroy();
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