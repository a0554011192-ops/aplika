package com.aplika.avraham;

import android.app.*;import android.os.*;import android.content.*;import android.content.pm.*;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.net.Uri;import android.provider.DocumentsContract;import android.view.*;import android.view.inputmethod.InputMethodManager;import android.widget.*;import java.io.*;import java.util.*;

public class MainActivity extends Activity{
 LinearLayout root; EditText input; TextView result; Button modeBtn,langBtn; boolean english=false; String mode="open"; OfflineEngine engine;
 final String[] openWords={"פתח","תפתח","לפתוח","פתיחה","open","launch","start","run"};
 final String[] actWords={"הפעל","תפעיל","הפעלתי","הדלק","תדליק","כבה","תכבה","סגור","תסגור","הגדר","תגדיר","שנה","תשנה","אפשר","תאפשר","בטל","תבטל","אפס","תאפס","open","enable","disable","configure","set","change","turn on","turn off"};
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(247,245,252));engine=new OfflineEngine(this);build();}

 TextView text(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(38,38,48));t.setPadding(16,10,16,10);return t;}
 GradientDrawable bg(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);g.setStroke(1,Color.rgb(226,222,238));return g;}
 Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}

 void build(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,20,20,14);root.setBackgroundColor(Color.rgb(247,245,252));
  LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  ImageView icon=new ImageView(this);icon.setImageResource(com.aplika.avraham.R.drawable.ic_avraham);head.addView(icon,new LinearLayout.LayoutParams(64,64));
  LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);TextView title=text("אברהם העברי",27);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);names.addView(title);names.addView(text("Android • Offline / אופליין",13));head.addView(names,new LinearLayout.LayoutParams(0,-2,1));
  langBtn=button("עברית / English");langBtn.setOnClickListener(v->{english=!english;applyLang();});head.addView(langBtn,new LinearLayout.LayoutParams(150,60));root.addView(head);
  modeBtn=button("מצב: פתיחה");modeBtn.setOnClickListener(v->chooseMode());root.addView(modeBtn,new LinearLayout.LayoutParams(-1,62));
  result=text("כתוב בקשה. המנוע בודק מילות מפתח, שגיאות כתיב והתאמות מקומיות.",16);result.setBackground(bg(Color.WHITE,24));root.addView(result,new LinearLayout.LayoutParams(-1,110));
  input=new EditText(this);input.setTextSize(17);input.setSingleLine(false);input.setMinLines(2);input.setHint("לדוגמה: פתח לי WhatsApp  /  Open Chrome");input.setPadding(18,12,18,12);input.setBackground(bg(Color.WHITE,24));root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bar=new LinearLayout(this);Button plus=button("＋"),minus=button("−"),go=button("בצע  /  Go"),files=button("תיקייה");plus.setTextSize(28);minus.setTextSize(28);
  plus.setOnClickListener(v->chooseMode());minus.setOnClickListener(v->{input.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
  go.setOnClickListener(v->process(input.getText().toString()));files.setOnClickListener(v->pickFolder());
  bar.addView(plus,new LinearLayout.LayoutParams(0,68,1));bar.addView(minus,new LinearLayout.LayoutParams(0,68,1));bar.addView(go,new LinearLayout.LayoutParams(0,68,2));bar.addView(files,new LinearLayout.LayoutParams(0,68,1));root.addView(bar);
  Button access=button("⚙ הגדרת קיצור + ואז −  /  Set shortcut");access.setOnClickListener(v->startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS")));root.addView(access,new LinearLayout.LayoutParams(-1,58));
  setContentView(root);applyLang();
 }

 void chooseMode(){PopupMenu p=new PopupMenu(this,modeBtn);p.getMenu().add("פתיחת אפליקציה / Open app");p.getMenu().add("פעולה בהגדרות / Settings action");p.getMenu().add("שיחה / Chat");p.setOnMenuItemClickListener(m->{String s=m.getTitle().toString();mode=s.startsWith("פתיחת")?"open":s.startsWith("פעולה")?"action":"chat";applyLang();return true;});p.show();}
 void applyLang(){getWindow().getDecorView().setLayoutDirection(english?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);modeBtn.setText((english?"Mode: ":"מצב: ")+(mode.equals("open")?(english?"Open app":"פתיחה"):mode.equals("action")?(english?"Settings action":"פעולה בהגדרות"):(english?"Chat":"שיחה")));langBtn.setText(english?"English / עברית":"עברית / English");input.setHint(english?"Example: Open Chrome / Launch Maps":"לדוגמה: פתח לי Chrome / פתח מפות");}
 boolean explicit(String q,String[] words){String x=q.toLowerCase(Locale.ROOT);for(String w:words)if(x.contains(w.toLowerCase(Locale.ROOT)))return true;return false;}
 String targetOf(String q){String x=q;for(String w:openWords) x=x.replaceAll("(?iu)\\b"+java.util.regex.Pattern.quote(w)+"\\b"," ");return x.trim();}

 void process(String q){q=q.trim();if(q.isEmpty()){result.setText(english?"Please type a request.":"כתוב בקשה.");return;}
  if(mode.equals("chat")){chat(q);return;}
  if(mode.equals("action")){runAction(q);return;}
  if(explicit(q,actWords)&&!explicit(q,openWords)){runAction(q);return;}
  openThing(q);
 }
 void chat(String q){OfflineEngine.Resp r=engine.bestResponse(q,english);if(r==null){result.setText(english?"I do not have a matching offline response. Try another wording.":"אין לי תגובה מתאימה במאגר המקומי. נסה לנסח אחרת.");return;}result.setText(english?r.en:r.he);}
 void runAction(String q){OfflineEngine.ActionEntry a=engine.bestAction(q);if(a==null){result.setText(english?"No settings action matched.":"לא נמצאה פעולת הגדרות מתאימה.");return;}try{startActivity(engine.settingIntent(a.setting));result.setText((english?"Opening settings: ":"פותח הגדרות: ")+(english?a.en:a.he));}catch(Exception e){result.setText(english?"This Android version does not expose that settings screen.":"גרסת Android הזו אינה חושפת את מסך ההגדרה הזה.");}}
 void openThing(String q){String target=targetOf(q);PackageManager pm=getPackageManager();ApplicationInfo best=null;String bn="";int bs=0;
  for(ApplicationInfo a:pm.getInstalledApplications(PackageManager.GET_META_DATA)){String n=pm.getApplicationLabel(a).toString();int s=Math.max(engine.score(target,n),engine.score(q,n));if(s>bs){bs=s;best=a;bn=n;}}
  if(best!=null&&bs>=2){Intent i=pm.getLaunchIntentForPackage(best.packageName);if(i!=null){startActivity(i);result.setText((english?"Opening ":"פותח ")+bn);return;}}
  searchSelectedFolder(target);
 }
 void pickFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,11);}
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==11&&c==RESULT_OK&&d!=null){getPreferences(MODE_PRIVATE).edit().putString("tree",d.getData().toString()).apply();try{getContentResolver().takePersistableUriPermission(d.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}result.setText(english?"Folder saved for offline search.":"התיקייה נשמרה לחיפוש אופליין.");}}
 void searchSelectedFolder(String target){String u=getPreferences(MODE_PRIVATE).getString("tree","");if(u.isEmpty()){Intent p=new Intent(Intent.ACTION_OPEN_DOCUMENT);p.addCategory(Intent.CATEGORY_OPENABLE);p.setType("*/*");startActivityForResult(p,12);result.setText(english?"No indexed folder. Choose a file.":"לא נבחרה תיקיית חיפוש. בחר מסמך.");return;}FileHit h=find(Uri.parse(u),target,0,new int[]{0});if(h!=null){Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(h.uri),h.mime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);result.setText((english?"Opening ":"פותח ")+h.name);}catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(h.uri)));}}else{result.setText(english?"No file matched the name within the selected folder.":"לא נמצא קובץ תואם בשם בתיקייה שנבחרה.");}}
 static class FileHit{String name,uri,mime;FileHit(String n,String u,String m){name=n;uri=u;mime=m;}}
 FileHit find(Uri tree,String target,int depth,int[] count){if(depth>12||count[0]>8000)return null;try{String docId=DocumentsContract.getTreeDocumentId(tree);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,docId);android.database.Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_FLAGS},null,null,null);if(c==null)return null;FileHit near=null;int ns=0;while(c.moveToNext()&&count[0]++<8000){String id=c.getString(0),name=c.getString(1),mime=c.getString(2);Uri u=DocumentsContract.buildDocumentUriUsingTree(tree,id);if(!"vnd.android.document/directory".equals(mime)){int s=Math.max(engine.score(target,name),engine.score(target,name.replaceFirst("(?s)\\.[^.]+$","")));if(s>ns){ns=s;near=new FileHit(name,u.toString(),mime);}}else if(depth<12){FileHit x=find(u,target,depth+1,count);if(x!=null)return x;}}c.close();return ns>=2?near:null;}catch(Exception e){return null;}}
}