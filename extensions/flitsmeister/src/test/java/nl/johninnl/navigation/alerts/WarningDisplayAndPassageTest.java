package nl.johninnl.navigation.alerts;
import static org.junit.Assert.*;
import android.os.Bundle;
import android.location.Location;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningDisplayAndPassageTest {
    final WarningSchedulerTest fixtures=new WarningSchedulerTest();
    WarningEvent state(String id,int priority,long revision){return fixtures.event(id,priority,revision,WarningEvent.Operation.UPSERT);}
    @Test public void multipleUrgentWarningsStayVisibleAndNonwarningContentIsExcluded(){
        WarningView view=new WarningView();WarningEvent camera=state("camera",1,1),behind=state("ambulance behind",0,1),person=state("person on road",0,1);
        Bundle b=WarningCodec.encode(state("parking",0,1));b.putString("category","display_only");view.events(Arrays.asList(camera,behind,person,WarningCodec.decode(b,1000)));
        String shown=view.messages(1100,e->true);assertTrue(shown.contains("ambulance behind"));assertTrue(shown.contains("person on road"));assertFalse(shown.contains("parking"));assertEquals(3,shown.split("\n").length);
    }
    @Test public void staleSnapshotCannotOverwriteANewerRevision(){
        WarningView view=new WarningView();WarningEvent old=state("road",1,1),current=state("road",1,2);view.events(Arrays.asList(old,current));
        view.snapshot(new WarningConnection.Snapshot(old.providerSession,old.sequence,false,true,"UNOWNED",new ArrayList<>(Arrays.asList(old)),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()));
        assertTrue(view.messages(1100,e->true).contains("revision 2"));assertEquals("",view.messages(31000,e->true));
    }
    @Test public void explicitPoliceSelectionMatchesTheActualPinnedNativeEnum(){
        android.content.SharedPreferences prefs=org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("native-police",0);prefs.edit().clear().putInt("waze_alerts_kinds",1).commit();WarningPreferences choices=new WarningPreferences(prefs,true);
        Bundle b=WarningCodec.encode(state("police",1,1));b.putString("native_type","POLICEHOTSPOT");assertTrue(choices.enabled(WarningCodec.decode(b,1000)));
        b.putString("category","emergency_vehicle");assertFalse(choices.enabled(WarningCodec.decode(b,1000)));
    }
    @Test public void pointPassageNeedsFreshDirectedEvidenceAndNeverFiltersEmergency(){
        Bundle b=WarningCodec.encode(state("point",1,1));b.putString("relevance","point_ahead");b.putDouble("latitude",0);b.putDouble("longitude",0);b.putLong("position_observed_elapsed_ms",1000);b.putBoolean("position_estimated",false);WarningEvent point=WarningCodec.decode(b,1000);
        Location fix=new Location("gps");fix.setLatitude(.001);fix.setLongitude(0);fix.setAccuracy(5);fix.setSpeed(10);fix.setBearing(0);fix.setElapsedRealtimeNanos(1000000000);
        assertTrue(WarningPassage.passed(point,fix,1100));assertFalse(WarningPassage.passed(point,fix,7000));fix.setLongitude(.001);assertFalse(WarningPassage.passed(point,fix,1100));fix.setLongitude(0);fix.setSpeed(0);assertFalse(WarningPassage.passed(point,fix,1100));
        assertFalse(WarningPassage.passedOnRoute(point,fix,1100,100d));assertTrue(WarningPassage.passedOnRoute(point,fix,1100,-100d));assertFalse(WarningPassage.passedOnRoute(point,fix,1100,null));
        b.putString("relevance","moving_emergency");assertFalse(WarningPassage.passed(WarningCodec.decode(b,1000),fix,1100));assertFalse(WarningPassage.passed(point,null,1100));
    }
    @Test public void producerSpecificFamiliesUseThePromisedAudibleCategory(){
        Bundle b=WarningCodec.encode(state("families",1,1));
        for(String family:new String[]{"service_vehicle","slow_vehicle","roadworks","weather","closure"}){
            b.putString("category",family);assertEquals(family,WarningPreferences.ROAD_DANGER,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        }
        b.putString("category","traffic_camera");assertEquals(WarningPreferences.CAMERA,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("category","police_control");assertEquals(WarningPreferences.POLICE,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
    }
    @Test public void wazeWarningCategoriesPreserveZoneRoadFeaturesAndStaticEmergencyHazards(){
        Bundle b=WarningCodec.encode(state("waze",1,1));
        b.putString("native_type","AVERAGE_SPEED_CAMERA");b.putString("category","average_speed_zone");
        assertEquals(WarningPreferences.ZONE,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("native_type","NOTIFICATION");b.putString("category","road_feature");
        assertEquals(WarningPreferences.ROAD_DANGER,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("native_type","HAZARD");b.putString("category","road_danger");b.putString("native_subtype","HAZARD_TYPE_EMERGENCY_VEHICLE");
        assertEquals(WarningPreferences.ROAD_DANGER,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("native_type","POLICE");b.putString("category","police_control");b.putString("native_subtype","POLICE_TYPE_MOBILE_CAMERA");
        assertEquals(WarningPreferences.CAMERA,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("native_subtype","POLICE_TYPE_HIDDEN");assertEquals(WarningPreferences.POLICE,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("category","other_native");b.putString("native_subtype","");
        for(String type:new String[]{"HAZARD","ACCIDENT","CONSTRUCTION","CLOSURE","BLOCKED_LANE","BAD_WEATHER","PERMANENT_HAZARD","ANIMAL","SUDDEN_SLOWDOWN","SPEED_BUMP","DANGEROUS_CURVE","SCHOOL_ZONE"}){
            b.putString("native_type",type);assertEquals(type,WarningPreferences.ROAD_DANGER,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        }
    }

    @Test public void wazeLegacyFineCategoryChoicesKeepCrashAndClosureMeaningWithoutASubtype(){
        android.content.SharedPreferences prefs=org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("waze-legacy-types",0);prefs.edit().clear().putInt("waze_alerts_kinds",1<<1).commit();
        WarningPreferences choices=new WarningPreferences(prefs,true,WarningProviders.WAZE);Bundle b=WarningCodec.encode(state("waze-legacy",1,1));b.putString("native_subtype","");b.putString("native_type","ACCIDENT");
        assertTrue(choices.enabled(WarningCodec.decode(b,1000)));b.putString("native_type","HAZARD");assertFalse(choices.enabled(WarningCodec.decode(b,1000)));
        prefs.edit().putInt("morphe_native_warnings_waze_legacy_types",1<<4).commit();b.putString("native_type","CONSTRUCTION");assertTrue(choices.enabled(WarningCodec.decode(b,1000)));b.putString("native_type","CLOSURE");assertTrue(choices.enabled(WarningCodec.decode(b,1000)));
    }

    @Test public void nativeCarVariantsAndExclusionsDoNotChangePhoneTextOrOrdinaryWarnings(){
        WarningView view=new WarningView();Bundle b=WarningCodec.encode(state("notification",1,1));b.putString("native_type","NOTIFICATION");b.putString("message","Phone cyclist title and detail");b.putBoolean("native_car_available",true);b.putString("native_car_title","Cyclist ahead");b.putString("native_car_body","Use caution");
        view.events(Collections.singletonList(WarningCodec.decode(b,1000)));
        assertEquals("Phone cyclist title and detail",view.messages(1000,event->true));assertEquals("Cyclist ahead: Use caution",view.carMessages(1000,event->true));
        view.clear();b.putBoolean("native_car_available",false);view.events(Collections.singletonList(WarningCodec.decode(b,1000)));
        assertEquals("Phone cyclist title and detail",view.messages(1000,event->true));assertEquals("",view.carMessages(1000,event->true));
        view.events(Collections.singletonList(state("camera",1,1)));assertEquals("Native camera revision 1",view.carMessages(1000,event->true));
        assertEquals("",view.carMessages(1000,event->false));
    }

}
