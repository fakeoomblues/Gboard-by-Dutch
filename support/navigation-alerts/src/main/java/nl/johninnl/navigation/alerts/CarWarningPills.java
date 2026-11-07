package nl.johninnl.navigation.alerts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import java.util.List;
import java.util.Locale;

/** App-rendered, independent pills bounded by the current host-supplied visible area. */
public final class CarWarningPills {
    private CarWarningPills() {}
    public static final float WIDTH=180,HEIGHT=64,GAP=10;
    public static float height(boolean zone,int warnings){int count=(zone?1:0)+warnings;return count==0?0:count*HEIGHT+(count-1)*GAP;}
    public static boolean zoneEvent(WarningEvent event){return (WarningPreferences.categoryOf(event)==WarningPreferences.ZONE||IndicatorContract.kind(event)==IndicatorContract.Kind.AVERAGE_SPEED)&&event.phase!=WarningEvent.Phase.EXIT&&!Boolean.TRUE.equals(event.fields().get("native_zone_alarm"));}
    public static String distance(Integer meters,boolean imperial){
        if(meters==null||meters<0)return "—";
        if(imperial)return meters<304.8?Math.round(meters*3.28084)+" ft":String.format(Locale.getDefault(),"%.1f mi",meters/1609.344);
        return meters<1000?meters+" m":String.format(Locale.getDefault(),"%.1f km",meters/1000d);
    }
    public static boolean fresh(Long at,long now){return WarningGeometry.fresh(at,now);}
    public static Float approachProgress(WarningEvent event,long now){
        WarningGeometry geometry=WarningGeometry.fromNative(event,now);return geometry==null?null:geometry.progress(event,now);
    }
    public static String relation(WarningEvent event,boolean dutch){
        return event.category.equals("emergency_vehicle")&&event.relationship.equals("behind")?(dutch?"Van achteren":"Behind"):"";
    }
    public static String relation(WarningEvent event,boolean dutch,long now){return WarningGeometry.relationshipKnown(event,now)?relation(event,dutch):"";}
    public static void draw(Context context,Canvas canvas,RectF area,float requestedScale,boolean dark,boolean imperial,
            boolean zone,Double averageKmh,Double limitKmh,Double progress,int remaining,boolean estimated,List<WarningEvent> events){
        draw(context,canvas,area,requestedScale,dark,imperial,zone,averageKmh,limitKmh,progress,remaining,estimated,events,null);
    }
    public static void draw(Context context,Canvas canvas,RectF area,float requestedScale,boolean dark,boolean imperial,
            boolean zone,Double averageKmh,Double limitKmh,Double progress,int remaining,boolean estimated,List<WarningEvent> events,Typeface nativeTypeface){
        draw(context,canvas,area,requestedScale,dark,imperial,zone,averageKmh,limitKmh,progress,remaining,estimated,events,nativeTypeface,null);
    }
    public static void draw(Context context,Canvas canvas,RectF area,float requestedScale,boolean dark,boolean imperial,
            boolean zone,Double averageKmh,Double limitKmh,Double progress,int remaining,boolean estimated,List<WarningEvent> events,Typeface nativeTypeface,java.util.Map<String,WarningGeometry> geometry){
        int count=events.size()+(zone?1:0);if(count==0||area.isEmpty())return;
        float scale=Math.min(requestedScale,Math.min(area.width()/WIDTH,area.height()/height(zone,events.size())));
        if(scale<.5f)return;
        int saved=canvas.save();canvas.clipRect(area);
        float left=area.right-WIDTH*scale,top=area.bottom-height(zone,events.size())*scale;
        canvas.translate(left,top);canvas.scale(scale,scale);
        Paint text=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface face=Typeface.create(nativeTypeface,Typeface.NORMAL);
        boolean dutch=context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("nl");
        long now=SystemClock.elapsedRealtime();
        for(WarningEvent event:events){
            card(canvas,fill,dark);WarningIcons.draw(context,canvas,event,new RectF(6,2,66,62));
            WarningGeometry point=geometry==null?WarningGeometry.fromNative(event,now):geometry.get(event.warningId);
            boolean known=point!=null&&fresh(point.observedAt,now);
            String relationship=relation(event,dutch,now);
            String value=IndicatorContract.valueText(event,imperial);
            String label=known?(point.estimated?"≈":"")+distance(point.distanceMeters,imperial):value==null?WarningView.carMessage(event).replace('\n',' '):value;
            text.setTypeface(face);text.setColor(dark?0xffffffff:0xff202124);text.setTextAlign(Paint.Align.LEFT);
            if(known||value!=null)distanceLabel(canvas,text,label,relationship.isEmpty()?31:35);
            else {
                text.setTextSize(16);
                if(text.measureText(label)>90){int chars=text.breakText(label,true,90-text.measureText("…"),null);label=label.substring(0,chars).trim()+"…";}
                canvas.drawText(label,74,31,text);
            }
            if(!relationship.isEmpty()){text.setTextSize(10);text.setTextAlign(Paint.Align.CENTER);canvas.drawText(relationship,119,13,text);}
            Float fraction=point==null?null:point.progress(event,now);if(fraction!=null)bar(canvas,fill,fraction,dark,0xff1684ff,74,164,44);
            canvas.translate(0,HEIGHT+GAP);
        }
        if(zone){
            card(canvas,fill,dark);int ink=dark?0xffffffff:0xff202124;
            text.setColor(ink);text.setTypeface(face);text.setTextSize(14);text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(dutch?"Gemiddeld":"Average",14,28,text);
            String value=averageKmh==null?"—":String.valueOf(Math.round(imperial?averageKmh/1.609344:averageKmh));String unit=imperial?"mph":"km/h";
            text.setTextSize(13);text.setTextAlign(Paint.Align.RIGHT);canvas.drawText(unit,166,28,text);float unitWidth=text.measureText(unit);
            text.setTextSize(22);
            if(averageKmh!=null&&limitKmh!=null&&averageKmh>limitKmh+5)text.setColor(0xffe8514a);
            canvas.drawText(value,160-unitWidth,28,text);
            if(progress!=null)bar(canvas,fill,(float)Math.max(0,Math.min(1,progress/100)),dark,0xff1684ff,14,166,42);
            text.setTextSize(11);text.setColor(ink);text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(progress==null?"—":(estimated?"≈":"")+Math.round(progress)+"%",14,57,text);
            text.setTextAlign(Paint.Align.RIGHT);canvas.drawText(distance(remaining,imperial),166,57,text);
        }
        canvas.restoreToCount(saved);
    }
    private static void distanceLabel(Canvas canvas,Paint text,String label,float baseline){
        int split=label.lastIndexOf(' ');String number=split<0?label:label.substring(0,split),unit=split<0?"":label.substring(split+1);
        float size=22,numberWidth,unitWidth;
        do{text.setTextSize(size);numberWidth=text.measureText(number);text.setTextSize(size*.72f);unitWidth=text.measureText(unit);if(numberWidth+unitWidth+4<=90||size<=16)break;size--;}while(true);
        float x=74+(90-numberWidth-unitWidth-4)/2;text.setTextAlign(Paint.Align.LEFT);text.setTextSize(size);canvas.drawText(number,x,baseline,text);
        text.setTextSize(size*.72f);canvas.drawText(unit,x+numberWidth+4,baseline,text);
    }
    private static void card(Canvas c,Paint p,boolean dark){p.setColor(dark?0xff20242b:0xffffffff);p.setShadowLayer(2,0,1,0x40000000);c.drawRoundRect(new RectF(1,1,WIDTH-1,HEIGHT-1),31,31,p);p.clearShadowLayer();}
    private static void bar(Canvas c,Paint p,float progress,boolean dark,int color,float left,float right,float top){
        p.setColor(dark?0xff697486:0xffd0d2d4);
        c.drawRoundRect(new RectF(left,top,right,top+6),3,3,p);
        p.setColor(color);c.drawRoundRect(new RectF(left,top,left+(right-left)*progress,top+6),3,3,p);
    }
}
