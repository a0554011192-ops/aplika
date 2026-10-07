package com.aplika.avraham;

import android.content.*;
import android.content.pm.*;
import java.io.*;
import java.util.*;

final class OfflineEngine {
 static final int APP_COUNT=3000,ACTION_COUNT=4000,RESPONSE_COUNT=5000,SYN_COUNT=6000;
 static class AppEntry{String he,en;AppEntry(String h,String e){he=h;en=e;}}
 static class ActionEntry{String he,en;int setting;String code;String[] triggers;ActionEntry(String h,String e,int s,String c,String[] t){he=h;en=e;setting=s;code=c;triggers=t;}}
 static class Resp{String he,en;String[] triggers;Resp(String h,String e,String[] t){he=h;en=e;triggers=t;}}
 final ArrayList<AppEntry> apps=new ArrayList<>();
 final ArrayList<ActionEntry> actions=new ArrayList<>();
 final ArrayList<Resp> responses=new ArrayList<>();
 final HashMap<String,String> synonyms=new HashMap<>();
 OfflineEngine(Context c){load(c,"apps.tsv",0);load(c,"actions.tsv",1);load(c,"responses.tsv",2);load(c,"synonyms.tsv",3);}
 void load(Context c,String fn,int type){
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open(fn),"UTF-8"))){
   String l;while((l=br.readLine())!=null){String[] p=l.split("\\t",-1);
    if(type==0&&p.length>=3)apps.add(new AppEntry(p[1],p[2]));
    else if(type==1&&p.length>=6)actions.add(new ActionEntry(p[1],p[2],Integer.parseInt(p[3]),p[4],p[5].split("\\|",-1)));
    else if(type==2&&p.length>=4)responses.add(new Resp(p[1],p[2],p[3].split("\\|",-1)));
    else if(type==3&&p.length>=3)synonyms.put(normalize(p[1]),normalize(p[2]));
   }
  }catch(Exception ignored){}
 }
 static String normalize(String s){return s.toLowerCase(Locale.ROOT).replace("׳","'").replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
 static String[] tokens(String s){String n=normalize(s);if(n.isEmpty())return new String[0];String[] a=n.split("\\s+");for(int i=0;i<a.length;i++){String x=synonyms.get(a[i]);if(x!=null)a[i]=x;}return a;}
 static int lev(String a,String b){if(a.equals(b))return 0;if(Math.abs(a.length()-b.length())>2)return 3;int[][]d=new int[a.length()+1][b.length()+1];for(int i=0;i<=a.length();i++)d[i][0]=i;for(int j=0;j<=b.length();j++)d[0][j]=j;for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++){d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));if(d[i][j]>2)d[i][j]=3;}return d[a.length()][b.length()];}
 boolean near(String a,String b){return a.equals(b)||lev(a,b)<=2;}
 int score(String q,String text){String[]qs=tokens(q),ts=tokens(text);int s=0;for(String a:qs){for(String b:ts){if(near(a,b)){s+=a.equals(b)?6:2;break;}}}return s;}
 AppEntry bestApp(String q){AppEntry best=null;int bs=0;for(AppEntry a:apps){int s=Math.max(score(q,a.he),score(q,a.en));if(s>bs){bs=s;best=a;}}return best;}
 ActionEntry bestAction(String q){ActionEntry best=null;int bs=1;for(ActionEntry a:actions){int s=Math.max(score(q,a.he),score(q,a.en));for(String tr:a.triggers)s=Math.max(s,score(q,tr));if(s>bs){bs=s;best=a;}}return best;}
 Resp bestResponse(String q,boolean en){Resp best=null;int bs=0;for(Resp r:responses)for(String tr:r.triggers){int s=score(q,tr);if(s>bs){bs=s;best=r;}}return best;}
 Intent settingIntent(int index){
  String[] a={"android.settings.WIFI_SETTINGS","android.settings.BLUETOOTH_SETTINGS","android.settings.AIRPLANE_MODE_SETTINGS","android.settings.NETWORK_OPERATOR_SETTINGS","android.settings.DATA_USAGE_SETTINGS","android.settings.TETHER_SETTINGS","android.settings.VPN_SETTINGS","android.settings.PRIVATE_DNS_SETTINGS","android.settings.DISPLAY_SETTINGS","android.settings.SOUND_SETTINGS","android.settings.NOTIFICATION_SETTINGS","android.settings.APPLICATION_SETTINGS","android.settings.STORAGE_SETTINGS","android.settings.SECURITY_SETTINGS","android.settings.LOCATION_SOURCE_SETTINGS","android.settings.ACCESSIBILITY_SETTINGS","android.settings.BATTERY_SAVER_SETTINGS","android.settings.BATTERY_OPTIMIZATION_SETTINGS","android.settings.DATE_SETTINGS","android.settings.TIME_SETTINGS","android.settings.TIMEZONE_SETTINGS","android.settings.LOCALE_SETTINGS","android.settings.INPUT_METHOD_SETTINGS","android.settings.CAST_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS"};return new Intent(a[Math.floorMod(index,a.length)]);
 }
}