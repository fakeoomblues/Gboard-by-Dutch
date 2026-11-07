package nl.johninnl.navigation.alerts;

import android.location.Location;

/** Display geometry only. Native event identity, observation and speech authority remain untouched. */
public final class WarningGeometry {
    public final int distanceMeters;
    public final long observedAt;
    public final boolean estimated;
    public final String origin;
    private WarningGeometry(int distance,long at,boolean estimated,String origin){distanceMeters=distance;observedAt=at;this.estimated=estimated;this.origin=origin;}
    public static boolean fresh(Long at,long now){return at!=null&&at>=0&&at<=now&&now-at<=5000;}
    /** An entry-point coordinate is not the end of a native segment already being traversed. */
    public static boolean remainingSegment(WarningEvent event){
        java.util.Map<String,Object> fields=event.fields();
        return "remaining_in_segment".equals(fields.get("native_distance_kind"))
            ||"InZone".equals(fields.get("native_zone_state"))||"LeftZone".equals(fields.get("native_zone_state"))
            ||Boolean.TRUE.equals(fields.get("native_segment_correlated"))&&"AVERAGE_SPEED_CAMERA".equals(fields.get("native_segment_type"))
                &&("WITHIN".equals(fields.get("native_segment_phase"))||"InZone".equals(fields.get("native_zone_state"))||"LeftZone".equals(fields.get("native_zone_state")));
    }
    public static boolean pointDisplay(WarningEvent event){return event.relevance.equals("point_ahead")&&!remainingSegment(event);}
    public static WarningGeometry fromNative(WarningEvent event,long now){
        if(!event.stateValid(now))return null;
        java.util.Map<String,Object> fields=event.fields();
        if(Boolean.TRUE.equals(fields.get("native_segment_correlated"))&&"AVERAGE_SPEED_CAMERA".equals(fields.get("native_segment_type"))){
            Object phase=fields.get("native_segment_phase"),state=fields.get("native_zone_state");
            boolean within="WITHIN".equals(phase)&&(state==null||"InZone".equals(state));
            boolean approach="APPROACHING".equals(phase)&&(state==null||"BeforeZone".equals(state));
            Object distance=fields.get("native_segment_distance_m"),at=fields.get("native_segment_observed_ms");
            if((within||approach)&&distance instanceof Integer&&(Integer)distance>=0&&at instanceof Long&&fresh((Long)at,now))
                return new WarningGeometry((Integer)distance,(Long)at,false,within?"native_segment_remaining":"native_segment_approach");
            // A known in-zone/left-zone observation cannot fall back to the entry-point distance.
            if("WITHIN".equals(phase)||"InZone".equals(state)||"LeftZone".equals(state))return null;
        }
        if(("InZone".equals(fields.get("native_zone_state"))||"LeftZone".equals(fields.get("native_zone_state")))
                &&!"remaining_in_segment".equals(fields.get("native_distance_kind")))return null;
        return event.stateValid(now)&&event.distanceMeters!=null&&fresh(event.distanceObservedAt,now)
            ?new WarningGeometry(event.distanceMeters,event.distanceObservedAt,event.distanceEstimated,
                "remaining_in_segment".equals(fields.get("native_distance_kind"))?"native_segment_remaining":"native"):null;
    }
    public static WarningGeometry display(WarningEvent event,Location fix,long now,Double routeDistance,boolean routeRequired){
        WarningGeometry nativeValue=fromNative(event,now);if(nativeValue!=null)return nativeValue;
        if(!event.stateValid(now)||!pointDisplay(event)||event.latitude==null||event.longitude==null
                ||!WarningPassage.freshFix(fix,now))return null;
        // Fixed source points do not move between updates. Other geometry must still be fresh.
        if(!Boolean.TRUE.equals(event.fields().get("native_point_target"))&&!fresh(event.positionObservedAt,now))return null;
        double distance;
        String origin;
        if(routeRequired){
            if(routeDistance==null||!Double.isFinite(routeDistance))return null;
            distance=routeDistance;origin="receiver_route";
        }else{
            if(!fix.hasBearing()||!Float.isFinite(fix.getBearing())
                    ||fix.hasBearingAccuracy()&&fix.getBearingAccuracyDegrees()>30)return null;
            Location target=new Location("warning-point");target.setLatitude(event.latitude);target.setLongitude(event.longitude);
            float bearing=fix.bearingTo(target);if(!Float.isFinite(bearing))return null;
            double difference=Math.abs((bearing-fix.getBearing()+540)%360-180);
            if(difference>60)return null;
            distance=fix.distanceTo(target);origin="receiver_heading";
        }
        if(!Double.isFinite(distance)||distance<0||distance>2000)return null;
        return new WarningGeometry((int)Math.round(distance),fix.getElapsedRealtimeNanos()/1000000,true,origin);
    }
    public Float progress(WarningEvent event,long now){
        if(!fresh(observedAt,now))return null;
        if(origin.equals("native_segment_remaining")){
            Object percent=event.fields().get("native_zone_percent"),at=event.fields().get("native_zone_observed_elapsed_ms");
            return percent instanceof Integer&&(Integer)percent>=0&&(Integer)percent<=100&&at instanceof Long&&fresh((Long)at,now)
                ?(Integer)percent/100f:null;
        }
        Integer span=event.triggerMeters;
        if(span==null||span<=0||!fresh(event.triggerObservedAt,now)){
            if(event.rangeMeters==null||!fresh(event.rangeObservedAt,now)||!"native_ui_max_distance".equals(event.fields().get("range_origin")))return null;
            span=event.rangeMeters;
        }
        return Math.max(0,Math.min(1,1f-distanceMeters/span.floatValue()));
    }
    public static boolean relationshipKnown(WarningEvent event,long now){return event.stateValid(now)&&!event.relationship.equals("unknown")&&fresh(event.relationshipObservedAt==null?event.observedAt:event.relationshipObservedAt,now);}
}
