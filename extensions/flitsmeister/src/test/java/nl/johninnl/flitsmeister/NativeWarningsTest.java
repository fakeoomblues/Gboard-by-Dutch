package nl.johninnl.flitsmeister;

import static org.junit.Assert.*;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32,manifest=Config.NONE)
public class NativeWarningsTest {
    private final List<Bundle> events = new ArrayList<>();
    private Context app;
    @Before public void before() { app=RuntimeEnvironment.getApplication(); NativeWarnings.resetForTest(app, events::add); }
    public static class BaseReport {
        public String getId(){return "danger-7";}
        public String getReportType(){return "INCIDENT";}
        public Location getLocation(){return null;}
        public String reportTitleText(Context c){return "Native title";}
        public String getShortDescription(Context c){return "Native description";}
    }
    public static class Incident extends BaseReport {
        public String getIncidentType(){return "WRONG_WAY_DRIVER";}
        public String getVoiceOnlyText(){return "Native voice-only warning";}
    }
    public static class EarlyWarningReport extends BaseReport {
        public String getEarlyWarningTypeEnum(){return "AMBULANCE";}
        public String getFreeText(){return "Ambulance behind you";}
        public String getCode(){return "emergency-7";}
        public String getIncidentType(){throw new AssertionError("EarlyWarningReport is not Incident");}
    }
    public static class ServiceVehicle extends BaseReport {
        @Override public String getReportType(){return "SERVICE_VEHICLE";}
        public String getType(){return "TOW_SERVICE";}
    }
    public static class CitsBaseReport extends BaseReport {}
    public static class CitsRoadWorks extends CitsBaseReport {
        public String getIncidentType(){throw new AssertionError("CITS is not Incident");}
    }
    public static class EarlyWarning {
        public String getTitle(){return "Impact attenuator";}
        public String getSubTitle(){return "Road work ahead";}
        public Location getLocation(){return null;}
    }
    public static class Alerted {
        private final Object report;
        Alerted(Object report){this.report=report;}
        public Object getReport(){return report;}
        public boolean isActive(){return true;}
    }
    private Bundle last(){return events.get(events.size()-1);}
    @Test public void nativeEntryExportsExactTextBeforeLocalMuteAndWithoutSpeedOrGeometry() {
        NativeWarnings.regular(null,new Incident(),true,false,"Spookrijder!",null);
        assertEquals("ANNOUNCE",last().getString("operation"));
        assertEquals("WRONG_WAY_DRIVER",last().getString("native_subtype"));
        assertEquals("Spookrijder!",last().getString("message"));
        assertEquals(0,last().getInt("priority"));
        assertEquals("unknown",last().getString("relationship"));
        assertFalse(last().containsKey("latitude"));
        assertFalse(last().getBoolean("native_local_mute_bypass"));
    }
    @Test public void modelFamilyPrecedesIncidentReportedType() {
        NativeWarnings.regular(null,new EarlyWarningReport(),true,true,"Ambulance behind you",null);
        assertEquals("EarlyWarningReport",last().getString("native_family"));
        assertEquals("AMBULANCE",last().getString("native_subtype"));
        assertEquals("unknown",last().getString("relationship"));
        NativeWarnings.regular(null,new CitsRoadWorks(),true,false,"Werkzaamheden",null);
        assertEquals("CitsRoadWorks",last().getString("native_family"));
        assertEquals("INCIDENT",last().getString("native_type"));
    }
    @Test public void trueNativeRepeatsIncrementButBusUpdatesNeverAnnounce() {
        Incident report=new Incident();
        NativeWarnings.regular(null,report,true,false,"Spookrijder!",null);
        long first=last().getLong("announcement_index");
        NativeWarnings.reports(Arrays.asList(new Alerted(report)));
        assertEquals("UPSERT",last().getString("operation"));
        assertEquals(first,last().getLong("announcement_index"));
        assertEquals("Spookrijder!",last().getString("message"));
        NativeWarnings.regular(null,report,true,false,"Spookrijder!",null);
        assertEquals(first+1,last().getLong("announcement_index"));
        NativeWarnings.reports(Collections.emptyList());
        assertEquals("CLEAR",last().getString("operation"));
    }
    @Test public void serviceSubtypeAndAuditedNativeResourceFallbackSurvive() {
        NativeWarnings.regular(null,new ServiceVehicle(),true,false,null,null);
        assertEquals("TOW_SERVICE",last().getString("native_subtype"));
        assertEquals("Native description",last().getString("message"));
        assertEquals("native_resource",last().getString("message_origin"));
    }
    public static class EmptyReport extends BaseReport {
        @Override public String getShortDescription(Context c){return "";}
        @Override public String reportTitleText(Context c){return "";}
    }
    @Test public void missingTextIsExplicitUnsupportedStateRatherThanInventedHazardSpeech() {
        NativeWarnings.regular(null,new EmptyReport(),true,false,null,null);
        assertEquals("UPSERT",last().getString("operation"));
        assertEquals("MISSING_NATIVE_TEXT",last().getString("capture_diagnostic"));
        assertEquals("",last().getString("message"));
    }
    @Test public void verifiedCitsProjectionDoesNotDuplicateItsEarlierNativeAnnouncement() {
        NativeWarnings.early(null,"AMBULANCE",new EarlyWarningReport(),null,"Ambulance achter u",true,null);
        int count=events.size();
        NativeWarnings.suppressNextProjection();
        NativeWarnings.presentation(app,new EarlyWarning(),true);
        assertEquals(count,events.size());
        NativeWarnings.presentation(app,new EarlyWarning(),true);
        assertEquals(count+1,events.size());
        assertEquals("ANNOUNCE",last().getString("operation"));
    }
    @Test public void presentationRefreshAndRemovalHaveNoSpeechAuthority() {
        NativeWarnings.presentation(app,new EarlyWarning(),false);
        assertEquals("UPSERT",last().getString("operation"));
        assertEquals(0,last().getLong("announcement_index"));
        NativeWarnings.presentation(app,null,false);
        assertEquals("CLEAR",last().getString("operation"));
    }
    @Test public void citsProjectionSuppressionMatchesOnlyTheActualConvertedObject() {
        EarlyWarning converted=new EarlyWarning();NativeWarnings.suppressProjection(converted);
        NativeWarnings.presentation(app,converted,true);assertTrue(events.isEmpty());
        NativeWarnings.presentation(app,converted,true);assertTrue(events.isEmpty());
        NativeWarnings.presentation(app,new EarlyWarning(),true);assertEquals(1,events.size());
    }
    @Test public void presentationRefreshPreservesItsIdentityWithoutCreatingAnAnnouncement() {
        NativeWarnings.presentation(app,new EarlyWarning(),true);String id=last().getString("warning_id");long index=last().getLong("announcement_index");
        NativeWarnings.presentation(app,new EarlyWarning(),false);
        assertEquals(id,last().getString("warning_id"));assertEquals(index,last().getLong("announcement_index"));assertEquals("UPSERT",last().getString("operation"));
    }
    @Test public void exitAnnouncementSurvivesItsNativeRemovalSnapshot() {
        Incident report=new Incident();
        NativeWarnings.regular(null,report,true,false,"Start",null);
        NativeWarnings.regular(null,report,false,false,"End",null);
        int count=events.size();
        NativeWarnings.reports(Collections.emptyList());
        assertEquals(count,events.size());
        assertEquals("exit",last().getString("phase"));
        NativeWarnings.stopped();
        assertEquals("CLEAR",last().getString("operation"));
    }
    @Test public void aNativeRepeatAfterClearKeepsMonotonicAnnouncementIdentity() {
        Incident report=new Incident();NativeWarnings.regular(null,report,true,false,"Danger",null);
        long first=last().getLong("announcement_index");NativeWarnings.reports(Arrays.asList(new Alerted(report)));NativeWarnings.reports(Collections.emptyList());
        assertEquals("CLEAR",last().getString("operation"));NativeWarnings.regular(null,report,true,false,"Danger",null);
        assertEquals(first+1,last().getLong("announcement_index"));
    }
    @Test public void nativeCallbacksAfterStopCannotCreateFreshWarningsUntilEngineStarts() {
        Incident report=new Incident();NativeWarnings.regular(null,report,true,false,"Danger",null);NativeWarnings.stopped();int count=events.size();
        NativeWarnings.regular(null,report,true,false,"Late callback",null);assertEquals(count,events.size());
        NativeWarnings.started();NativeWarnings.regular(null,report,true,false,"Fresh decision",null);assertEquals(count+1,events.size());
    }
}
