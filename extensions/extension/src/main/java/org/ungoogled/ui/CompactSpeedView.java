package org.ungoogled.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Supplementary warnings and observed average-zone progress; Maps draws speed and limit. */
public final class CompactSpeedView extends View {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TelemetryStore.View data=TelemetryStore.View.empty("");
    private Double limit;
    private boolean imperial,dark=true;
    private String warning="";
    private final float density;
    private float totalHeight;
    private int bottomClearance;
    private android.text.StaticLayout warningLayout;
    private boolean carPills;
    private Typeface carTypeface;
    private java.util.Map<String,nl.johninnl.navigation.alerts.WarningGeometry> carGeometry=java.util.Collections.emptyMap();
    private java.util.List<nl.johninnl.navigation.alerts.WarningEvent> carEvents=java.util.Collections.emptyList();
    public CompactSpeedView(Context context) {
        super(context);density=getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void refresh(boolean dark) {
        bottomClearance=0;
        bind(ExternalTelemetry.view(),ExternalTelemetry.limitKmh(),PowerSaving.miles(),dark,ExternalTelemetry.compactWarning());
    }
    void bind(TelemetryStore.View snapshot,Double maximum,boolean imperial,boolean dark,String warning) {
        carPills=false;carEvents=java.util.Collections.emptyList();
        data=snapshot;limit=maximum;this.imperial=imperial;this.dark=dark;this.warning=warning;
        String average=!data.zoneActive?"":data.averageKmh==null?"Observed partial average pending":"Observed partial average "+Math.round(imperial?data.averageKmh/1.609344:data.averageKmh)+(imperial?" mph":" km/h");
        String progress=data.zoneActive?", "+(data.progressEstimated?"approximately ":"")+Math.round(data.progressPercent==null?0:data.progressPercent)+" percent, "+ExternalTelemetry.distance(data.remainingMeters,imperial)+" remaining":"";
        setContentDescription(average+progress+(average.isEmpty()||warning.isEmpty()?"":", ")+warning);
        requestLayout();invalidate();
    }
    void bindCar(TelemetryStore.View snapshot,Double maximum,boolean imperial,boolean dark,String legacy,java.util.List<nl.johninnl.navigation.alerts.WarningEvent> events){
        bind(snapshot,maximum,imperial,dark,legacy);carEvents=events;carPills=legacy.isEmpty();
        String description=getContentDescription().toString();long now=android.os.SystemClock.elapsedRealtime();
        for(nl.johninnl.navigation.alerts.WarningEvent event:events){
            description+=", "+nl.johninnl.navigation.alerts.WarningView.carMessage(event);
            nl.johninnl.navigation.alerts.WarningGeometry geometry=carGeometry.get(event.warningId);
            if(geometry!=null&&nl.johninnl.navigation.alerts.WarningGeometry.fresh(geometry.observedAt,now))description+=" · "+(geometry.estimated?"≈":"")+nl.johninnl.navigation.alerts.CarWarningPills.distance(geometry.distanceMeters,imperial);
        }
        setContentDescription(description);requestLayout();invalidate();
    }
    void carTypeface(Typeface face){carTypeface=face;}
    void carGeometry(java.util.Map<String,nl.johninnl.navigation.alerts.WarningGeometry> geometry){carGeometry=geometry;}
    boolean hasContent(){return data.zoneActive||!warning.isEmpty()||!carEvents.isEmpty();}
    private float dp(float value){return value*density;}
    private float sp(float value){return value*getResources().getDisplayMetrics().scaledDensity;}
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        if(!hasContent()){setMeasuredDimension(0,0);return;}
        if(carPills){
            setMeasuredDimension(resolveSize(Math.round(dp(nl.johninnl.navigation.alerts.CarWarningPills.WIDTH)),widthSpec),
                resolveSize(Math.round(dp(nl.johninnl.navigation.alerts.CarWarningPills.height(data.zoneActive,carEvents.size()))),heightSpec));return;
        }
        float fontScale=Math.max(1,getResources().getDisplayMetrics().scaledDensity/density);
        float desiredWidth=(warning.isEmpty()?dp(200):dp(280))*Math.min(1.5f,fontScale);
        int width=resolveSize((int)Math.ceil(desiredWidth),widthSpec);
        warningLayout=null;
        if(!warning.isEmpty()){
            android.text.TextPaint text=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);text.setTextSize(sp(13));text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));text.setColor(dark?0xffffd482:0xff3b4350);
            warningLayout=android.text.StaticLayout.Builder.obtain(warning,0,warning.length(),text,Math.max(1,width-Math.round(dp(20))))
                .setIncludePad(false).setLineSpacing(dp(2),1).setMaxLines(4).setEllipsize(android.text.TextUtils.TruncateAt.END).build();
        }
        totalHeight=dp(16)+(data.zoneActive?sp(22)+sp(11)+dp(16):0)+(data.zoneActive&&warningLayout!=null?dp(8):0)+(warningLayout==null?0:warningLayout.getHeight());
        setMeasuredDimension(width,resolveSize((int)Math.ceil(totalHeight+dp(bottomClearance)),heightSpec));
    }
    @Override protected void onDraw(Canvas canvas) {
        if(carPills){
            nl.johninnl.navigation.alerts.CarWarningPills.draw(getContext(),canvas,new RectF(0,0,getWidth(),getHeight()),density,dark,imperial,
                data.zoneActive,data.averageKmh,limit,data.progressPercent,data.remainingMeters,data.progressEstimated,carEvents,carTypeface,carGeometry);return;
        }
        super.onDraw(canvas);float x=dp(10),y=dp(8),width=Math.max(0,getWidth()-dp(20));
        int normal=dark?Color.WHITE:0xff111d2d,error=0xffe8514a;
        // Both the average and the warning need their own contrast, independent of map tiles.
        fill.setStyle(Paint.Style.FILL);fill.setColor(dark?0xff20242b:0xfff5f7fa);
        canvas.drawRoundRect(new RectF(dp(2),dp(2),getWidth()-dp(2),getHeight()-dp(2+bottomClearance)),dp(24),dp(24),fill);
        ink.setTextAlign(Paint.Align.LEFT);ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));ink.setColor(normal);
        ink.clearShadowLayer();
        if(data.zoneActive) {
            boolean avgOver=data.averageKmh!=null&&limit!=null&&data.averageKmh>limit+5;
            ink.setColor(normal);ink.setTextSize(sp(13));
            y+=sp(22)+dp(3);canvas.drawText("Average",x,y,ink);
            String average=data.averageKmh==null?"—":String.valueOf(Math.round(imperial?data.averageKmh/1.609344:data.averageKmh));
            String line=average+(imperial?" mph":" km/h");
            ink.setColor(avgOver?error:normal);ink.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));ink.setTextSize(sp(20));ink.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(line,x+width,y,ink);ink.setTextAlign(Paint.Align.LEFT);ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
            fill.setStyle(Paint.Style.FILL);fill.setColor(dark?0xff697486:0xffb6bec7);y+=dp(5);
            canvas.drawRoundRect(new RectF(x,y,x+width,y+dp(3)),dp(2),dp(2),fill);
            fill.setColor(avgOver?error:0xff287cf6);
            float progress=data.progressPercent==null?0:Math.max(0,Math.min(1,data.progressPercent.floatValue()/100));
            canvas.drawRoundRect(new RectF(x,y,x+width*progress,y+dp(3)),dp(2),dp(2),fill);
            y+=sp(11)+dp(7);ink.setTextSize(sp(11));ink.setColor(normal);
            canvas.drawText((data.progressEstimated?"≈":"")+Math.round(data.progressPercent==null?0:data.progressPercent)+"%",x,y,ink);
            ink.setTextAlign(Paint.Align.RIGHT);canvas.drawText(ExternalTelemetry.distance(data.remainingMeters,imperial),x+width,y,ink);ink.setTextAlign(Paint.Align.LEFT);
        }
        if(warningLayout!=null) {
            if(data.zoneActive)y+=dp(8);
            int saved=canvas.save();canvas.translate(x,y);warningLayout.draw(canvas);canvas.restoreToCount(saved);
        }
    }
}
