package com.aplika.avraham;

import android.content.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class OfflineEngine {
 static final int APP_COUNT=4000,ACTION_COUNT=712,RESPONSE_COUNT=9000,SYN_COUNT=516;

 static class Resp{
  String he,en;String[] triggers;String[] triggerNorms;
  Resp(String h,String e,String[] t){he=h;en=e;triggers=t;triggerNorms=new String[t.length];for(int i=0;i<t.length;i++)triggerNorms[i]=normalize(t[i]);}
 }
 static class ActionEntry{
  String he,en;int setting;String code;String[] triggers;
  ActionEntry(String h,String e,int s,String c,String[] t){he=h;en=e;setting=s;code=c;triggers=t;}
 }
 static class AppEntry{String he,en;AppEntry(String h,String e){he=h;en=e;}}

 final ArrayList<Resp> responses=new ArrayList<>();
 final ArrayList<ActionEntry> actions=new ArrayList<>();
 final Map<String,String> synonyms=new ConcurrentHashMap<>();
 final Map<String,String> commonAliases=new ConcurrentHashMap<>();

 final HashMap<String,ArrayList<Resp>> responseGroups=new HashMap<>();
 final HashSet<String> loadedResponseKeys=new HashSet<>();
 final HashMap<String,ArrayList<ActionEntry>> actionIndex=new HashMap<>();

 volatile boolean chatLoaded=false,commandsLoaded=false,appsLoaded=false,synonymsLoaded=false;

 OfflineEngine(Context c){
  alias("כרום","chrome");alias("גוגל כרום","google chrome");
  alias("וואטסאפ","whatsapp");alias("ווטסאפ","whatsapp");
  alias("יוטיוב","youtube");alias("מפות","maps");
  alias("וויז","waze");alias("ווייז","waze");
  alias("טלגרם","telegram");alias("דיסקורד","discord");
  alias("אינסטגרם","instagram");alias("טיקטוק","tiktok");
  alias("פייסבוק","facebook");alias("ספוטיפיי","spotify");
  alias("נטפליקס","netflix");alias("גימייל","gmail");alias("ג׳ימייל","gmail");
  alias("דרייב","google drive");alias("גוגל דרייב","google drive");
  alias("תמונות","google photos");alias("גוגל תמונות","google photos");
  alias("קבצים","files");alias("סייר קבצים","files");alias("סייר הקבצים","files");
  alias("מנהל קבצים","files");alias("מנהל הקבצים","files");alias("קבצים שלי","files");
  alias("file manager","files");alias("file explorer","files");alias("my files","files");alias("files","files");
  alias("מצלמה","camera");alias("camera","camera");
  alias("גלריה","gallery");alias("הגלריה","gallery");alias("תמונות","gallery");alias("גלריית תמונות","gallery");
  alias("photo gallery","gallery");alias("gallery","gallery");alias("photos","gallery");
  alias("שעון","clock");alias("השעון","clock");alias("clock","clock");alias("alarm clock","clock");alias("alarms","clock");
  alias("מחשבון","calculator");alias("המחשבון","calculator");alias("calculator","calculator");
  alias("נגן","music");alias("נגן מוזיקה","music");alias("נגן מוסיקה","music");alias("נגן המדיה","media player");
  alias("נגן מדיה","media player");alias("music player","music");alias("media player","media player");
  alias("player","media player");alias("מוזיקה","music");alias("music","music");
  alias("יומן","calendar");alias("לוח שנה","calendar");alias("calendar","calendar");
  alias("דפדפן","browser");alias("browser","browser");alias("אינטרנט","browser");alias("web browser","browser");
  alias("חנות","play store");alias("חנות play","play store");alias("חנות גוגל","play store");
  alias("גוגל פליי","play store");alias("גוגל פלי","play store");alias("גוגל play","play store");
  alias("google play","play store");alias("google play store","play store");alias("play store","play store");
  alias("play","play store");alias("store","play store");alias("פליי","play store");alias("פלי","play store");
  alias("דרייב","google drive");alias("גוגל דרייב","google drive");alias("drive","google drive");alias("google drive","google drive");
  alias("google drive","google drive");alias("google docs","google docs");alias("docs","google docs");
  alias("google sheets","google sheets");alias("sheets","google sheets");
  alias("google photos","google photos");alias("photos","google photos");
  alias("מוזיקה","music");alias("שירים","music");alias("דואר","email");
  alias("הגדרות","settings");
 }

 void alias(String a,String c){commonAliases.put(normalize(a),normalize(c));}
 void catalogAlias(String a,String c){
  String k=normalize(a),v=normalize(c);
  if(k.isEmpty()||v.isEmpty())return;
  if(!commonAliases.containsKey(k))commonAliases.put(k,v);
 }

 synchronized void loadChat(Context c){
  if(chatLoaded)return;
  try{
   // Chat responses do not need the 6,000-row synonym catalog during load.
   // Keeping this path tiny makes the first answer available quickly.
   responses.clear();
   loadedResponseKeys.clear();
   responseGroups.clear();
   load(c,"responses.tsv",2);
   if(responses.size()!=RESPONSE_COUNT) throw new IOException("responses.tsv expected "+RESPONSE_COUNT+" rows, got "+responses.size());
   if(responseGroups.size()!=200) throw new IOException("responses.tsv expected 200 chat groups, got "+responseGroups.size());
   for(Map.Entry<String,ArrayList<Resp>> group:responseGroups.entrySet()){
    if(group.getValue().isEmpty())throw new IOException("empty chat group: "+group.getKey());
   }
   chatLoaded=true;
  }catch(Exception ex){
   chatLoaded=false;
   android.util.Log.e("Avraham","Failed to load chat catalog",ex);
  }
 }

 synchronized void loadSynonyms(Context c){
  if(synonymsLoaded)return;
  try{
   synonyms.clear();
   load(c,"synonyms.tsv",3);
   synonymsLoaded=true;
  }catch(Exception ex){
   synonymsLoaded=false;
   android.util.Log.e("Avraham","Failed to load synonym catalog",ex);
  }
 }

 synchronized void loadCommands(Context c){
  if(commandsLoaded)return;
  actions.clear();
  actionIndex.clear();
  try{
   loadSynonyms(c);
   load(c,"actions.tsv",1);
   commandsLoaded=true;
  }catch(Exception ex){
   commandsLoaded=false;
   android.util.Log.e("Avraham","Failed to load action catalog",ex);
  }
 }
 // Load the app catalog only as a compact alias table. Launching still uses the
 // real PackageManager list, so a catalog entry can never invent an installed app.
 boolean isKnownAppAlias(String s){
  String x=normalize(s);
  if(x.isEmpty())return false;
  if(commonAliases.containsKey(x))return true;
  String canon=canonicalWord(x);
  return !canon.equals(x) || synonyms.containsKey(x);
 }

 synchronized void loadApps(Context c){
  if(appsLoaded)return;
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("apps.tsv"),"UTF-8"),65536)){
   String l;
   while((l=br.readLine())!=null){
    String[] p=l.split("\\t",-1);
    if(p.length>=4){
     String he=normalize(p[1]), en=normalize(p[2]);
     String canon=en.isEmpty()?he:en;
     if(!he.isEmpty())catalogAlias(he,canon);
     if(!en.isEmpty())catalogAlias(en,canon);
     for(String t:p[3].split("\\|",-1)){
      if(!normalize(t).isEmpty())catalogAlias(t,canon);
     }
    }
   }
  }catch(Exception ex){
   appsLoaded=false;
   android.util.Log.e("Avraham","Failed to load app catalog",ex);
   return;
  }
  appsLoaded=true;
 }


 void load(Context c,String fn,int type){
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open(fn),"UTF-8"),65536)){
   String l;
   while((l=br.readLine())!=null){
    String[] p=l.split("\\t",-1);
    if(type==1&&p.length>=6){
     try{
      ActionEntry a=new ActionEntry(p[1],p[2],Integer.parseInt(p[3]),p[4],p[5].split("\\|",-1));
      actions.add(a);
      indexAction(a.he,a);indexAction(a.en,a);
      for(String t:a.triggers)indexAction(t,a);
     }catch(Exception ignored){}
    }else if(type==2){
     int a=l.indexOf('\t'), b=a<0?-1:l.indexOf('\t',a+1), d=b<0?-1:l.indexOf('\t',b+1);
     if(d>0){
      int idStart=a+1, heStart=b<0?0:a+1, enStart=b+1, trStart=d+1;
      String he=l.substring(heStart,enStart-1);
      String en=l.substring(enStart,d);
      String tr=l.substring(trStart);
      String responseKey=he+"\u0000"+en+"\u0000"+tr;
      if(!loadedResponseKeys.add(responseKey))continue;
      Resp rr=new Resp(he,en,tr.split("\\|",-1));
      responses.add(rr);
      for(String t:rr.triggerNorms){
       String n=normalize(t);
       if(n.isEmpty())continue;
       ArrayList<Resp> group=responseGroups.get(n);
       if(group==null){group=new ArrayList<>();responseGroups.put(n,group);}
       group.add(rr);
      }
     }
    }else if(type==3&&p.length>=3){
     synonyms.put(normalize(p[1]),normalize(p[2]));
    }
   }
  }catch(Exception ignored){}
 }

 // Chat matching deliberately works on the 200 unique conversation triggers,
 // not on all 9,000 response rows. Each trigger owns its 45 response variants.
 void indexResponse(String raw,Resp r){
  String n=normalize(raw);
  if(n.isEmpty())return;
  ArrayList<Resp> group=responseGroups.get(n);
  if(group==null){group=new ArrayList<>();responseGroups.put(n,group);}
  group.add(r);
 }
 void indexResponseNormalized(String n,Resp r){indexResponse(n,r);}

 void indexAction(String raw,ActionEntry a){
  for(String tok:tokenizeCanonical(raw)){
   if(tok.length()<2)continue;
   ArrayList<ActionEntry> list=actionIndex.get(tok);
   if(list==null){list=new ArrayList<>();actionIndex.put(tok,list);}
   if(!list.contains(a))list.add(a);
  }
 }

 static String normalize(String s){
  if(s==null||s.isEmpty())return "";
  String lower=s.toLowerCase(Locale.ROOT);
  StringBuilder out=new StringBuilder(lower.length());
  boolean pendingSpace=false;
  int hRun=0;

  for(int i=0;i<lower.length();i++){
   char c=lower.charAt(i);
   if(Character.isLetterOrDigit(c)){
    if(pendingSpace&&out.length()>0)out.append(' ');
    pendingSpace=false;
    if(c=='ח'){
     if(hRun<3)out.append(c);
     hRun++;
    }else{
     hRun=0;
     out.append(c);
    }
   }else{
    pendingSpace=true;
    hRun=0;
   }
  }

  // Preserve the previous "lol" collapsing behavior without regex: only
  // collapse tokens made entirely of repeated "lol".
  String normalized=out.toString().trim();
  if(normalized.isEmpty())return normalized;
  int start=0;
  StringBuilder finalText=new StringBuilder(normalized.length());
  while(start<normalized.length()){
   int end=normalized.indexOf(' ',start);
   if(end<0)end=normalized.length();
   String token=normalized.substring(start,end);
   if(token.length()>=6&&token.length()%3==0){
    boolean repeated=true;
    for(int i=0;i<token.length();i+=3){
     if(!token.regionMatches(i,"lol",0,3)){repeated=false;break;}
    }
    if(repeated)token="lol";
   }
   if(finalText.length()>0)finalText.append(' ');
   finalText.append(token);
   start=end+1;
  }
  return finalText.toString();
 }

 String canonicalWord(String w){
  String x=commonAliases.get(w);
  if(x==null)x=synonyms.get(w);
  return x==null?w:x;
 }

 ArrayList<String> phraseAliases=new ArrayList<>();

 String[] tokenizeCanonical(String s){
  String n=normalize(s);
  if(n.isEmpty())return new String[0];
  String[] w=n.split("\\s+");
  ArrayList<String> out=new ArrayList<>();
  for(int i=0;i<w.length;){
   String best=null,bestCanon=null;
   for(int len=Math.min(5,w.length-i);len>=2;len--){
    StringBuilder b=new StringBuilder();
    for(int j=0;j<len;j++){if(j>0)b.append(' ');b.append(w[i+j]);}
    String phrase=b.toString();
    String canon=commonAliases.get(phrase);
    if(canon==null)canon=synonyms.get(phrase);
    if(canon!=null){best=phrase;bestCanon=canon;break;}
   }
   if(best!=null){
    for(String x:normalize(bestCanon).split("\\s+"))out.add(x);
    i+=best.split(" ").length;
   }else{
    out.add(canonicalWord(w[i]));
    i++;
   }
  }
  return out.toArray(new String[0]);
 }

 static int edit(String a,String b){
  if(a.equals(b))return 0;
  if(Math.abs(a.length()-b.length())>2)return 3;
  int[][] d=new int[a.length()+1][b.length()+1];
  for(int i=0;i<=a.length();i++)d[i][0]=i;
  for(int j=0;j<=b.length();j++)d[0][j]=j;
  for(int i=1;i<=a.length();i++){
   for(int j=1;j<=b.length();j++){
    int v=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
    d[i][j]=Math.min(v,3);
   }
  }
  return d[a.length()][b.length()];
 }

 int score(String q,String text){return keywordScore(q,text);}
 
 int keywordScore(String query,String trigger){
  return fastScoreNormalized(normalize(query),normalize(trigger));
 }

 int fastScoreNormalized(String qn,String tn){
  if(qn.isEmpty()||tn.isEmpty())return 0;
  if(qn.equals(tn))return 100;
  if(tn.startsWith(qn) || tn.contains(" "+qn+" ") || tn.endsWith(" "+qn))return 82;
  if(qn.startsWith(tn+" ") || qn.contains(" "+tn+" ") || qn.endsWith(" "+tn))return 78;
  String[] q=qn.split("\\s+"), t=tn.split("\\s+");
  int hits=0;
  for(String a:q){
   if(a.length()<2)continue;
   for(String b:t){
    if(a.equals(b)){hits++;break;}
    if(a.length()>=3&&b.length()>=3&&(a.startsWith(b)||b.startsWith(a))){hits++;break;}
   }
  }
  if(hits==q.length && hits>0)return 60+Math.min(30,hits*5);
  return hits*10;
 }

 Resp quickResponse(String q){
  String n=normalize(q);
  if(n.isEmpty())return null;
  if(hasAny(n,"מה השם שלך","השם שלך","איך קוראים לך","who are you","what is your name"))
   return new Resp("השם שלי הוא אברהם העברי.","My name is Avraham HaIvri.",new String[]{"השם שלך"});
  if(hasAny(n,"מה אתה יכול לעשות","מה אתה יודע","יכולות","capabilities","what can you do"))
   return new Resp("אני יכול לענות ממאגר מקומי, לזהות מילות מפתח, לפתוח אפליקציות ולבצע פעולות מערכת נתמכות.","I can answer from a local catalog, detect keywords, open apps, and run supported system actions.",new String[]{"יכולות"});
  if(hasAny(n,"תן לי חידה","תתן לי חידה","חידה","riddle","give me a riddle"))
   return new Resp("הנה חידה: מה יש לו מקשים אבל לא דלתות? מקלדת. אם פתרת, נוודא יחד את התשובה.","Here is a riddle: What has keys but no doors? A keyboard. Check the answer after you think.",new String[]{"חידה"});
  if(hasAny(n,"בדיחה","תספר בדיחה","תן לי בדיחה","joke","tell me a joke"))
   return new Resp("בדיחה: למה המחשב הלך לרופא? כי היו לו יותר מדי חלונות פתוחים.","Joke: Why did the computer go to the doctor? It had too many open windows.",new String[]{"בדיחה"});
  if(hasAny(n,"עזרה","תעזור לי","איך משתמשים","help"))
   return new Resp("כתוב כמו שאתה מדבר. למשל: „תתן לי חידה מצחיקה”, „מה השם שלך?”, „פתח לי כרום” או „תגביה שמע”.","Write naturally. For example: “give me a funny riddle”, “what is your name?”, “open Chrome”, or “turn up the volume.”",new String[]{"עזרה"});
  return null;
 }

 boolean hasAny(String q,String... words){
  String n=normalize(q);
  for(String w:words)if(n.contains(normalize(w)))return true;
  return false;
 }

 Resp bestResponse(String q,boolean en){
  if(!chatLoaded)return null;
  return bestResponseIndexed(q);
 }

 Resp bestResponseIndexed(String q){
  String n=normalize(q);
  if(n.isEmpty()||responseGroups.isEmpty())return null;

  // Exact trigger: O(1), then a random variant from its 45 responses.
  ArrayList<Resp> exact=responseGroups.get(n);
  if(exact!=null&&!exact.isEmpty())return randomResponse(exact);

  String[] words=n.split("\\s+");
  StringBuilder cb=new StringBuilder();
  for(String w:words){
   String canon=canonicalWord(w);
   if(cb.length()>0)cb.append(' ');
   cb.append(canon);
  }
  String canonicalQuery=cb.toString().trim();
  if(!canonicalQuery.equals(n)){
   ArrayList<Resp> canonicalExact=responseGroups.get(canonicalQuery);
   if(canonicalExact!=null&&!canonicalExact.isEmpty())return randomResponse(canonicalExact);
  }

  // Fuzzy matching is over only the 200 unique triggers, never the 9,000
  // response rows. This keeps chat CPU work tiny and deterministic.
  String bestTrigger=null;
  int bestScore=0;
  ArrayList<String> ties=new ArrayList<>();
  for(String trigger:responseGroups.keySet()){
   int score=fastScoreNormalized(n,trigger);
   if(!canonicalQuery.equals(n))score=Math.max(score,fastScoreNormalized(canonicalQuery,trigger));
   if(score>bestScore){bestScore=score;bestTrigger=trigger;ties.clear();ties.add(trigger);}
   else if(score==bestScore&&score>=20)ties.add(trigger);
  }
  if(bestScore<20)return null;
  if(!ties.isEmpty())bestTrigger=ties.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(ties.size()));
  ArrayList<Resp> group=responseGroups.get(bestTrigger);
  return randomResponse(group);
 }

 Resp randomResponse(ArrayList<Resp> list){
  if(list==null||list.isEmpty())return null;
  return list.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(list.size()));
 }

 ActionEntry bestAction(String q){
  if(!commandsLoaded)return null;
  String[] qt=tokenizeCanonical(q);
  LinkedHashSet<ActionEntry> candidates=new LinkedHashSet<>();
  for(String tok:qt){ArrayList<ActionEntry> list=actionIndex.get(tok);if(list!=null)candidates.addAll(list);}
  ActionEntry best=null;int bestScore=0;
  for(ActionEntry a:candidates){
   int s=keywordScore(q,a.he)+keywordScore(q,a.en);
   for(String tr:a.triggers)s=Math.max(s,keywordScore(q,tr)+8);
   if(s>bestScore){bestScore=s;best=a;}
  }
  return bestScore>=10?best:null;
 }

 String canonical(String s){return canonicalWord(normalize(s));}

 Intent settingIntent(int index){
  String[] a={"android.settings.WIFI_SETTINGS","android.settings.BLUETOOTH_SETTINGS","android.settings.AIRPLANE_MODE_SETTINGS","android.settings.NETWORK_OPERATOR_SETTINGS","android.settings.DATA_USAGE_SETTINGS","android.settings.TETHER_SETTINGS","android.settings.VPN_SETTINGS","android.settings.PRIVATE_DNS_SETTINGS","android.settings.DISPLAY_SETTINGS","android.settings.SOUND_SETTINGS","android.settings.NOTIFICATION_SETTINGS","android.settings.APPLICATION_SETTINGS","android.settings.STORAGE_SETTINGS","android.settings.SECURITY_SETTINGS","android.settings.LOCATION_SOURCE_SETTINGS","android.settings.ACCESSIBILITY_SETTINGS","android.settings.BATTERY_SAVER_SETTINGS","android.settings.BATTERY_OPTIMIZATION_SETTINGS","android.settings.DATE_SETTINGS","android.settings.TIME_SETTINGS","android.settings.TIMEZONE_SETTINGS","android.settings.LOCALE_SETTINGS","android.settings.INPUT_METHOD_SETTINGS","android.settings.CAST_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS"};
  return new Intent(a[Math.floorMod(index,a.length)]);
 }
}