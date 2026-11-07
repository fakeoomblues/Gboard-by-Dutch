package nl.johninnl.vela;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32, qualifiers="en-rUS")
public class CarUiTest {
    @Before public void init() { TelemetryRuntime.init((Application)RuntimeEnvironment.getApplication(),()->{}); }
    @After public void reset() { TelemetryRuntime.resetForTest(); }

    @Test public void overlaysUseCurrentVisibleBoundsInsteadOfTheLargerStableRegion() {
        Rect visible=new Rect(420,90,1090,448),stable=new Rect(12,12,1178,448);
        Rect actual=CarUi.overlayViewport(visible,stable,1190,460);
        assertEquals(visible,actual);assertNotSame(visible,actual);
        assertEquals(new Rect(12,12,1178,448),stable);
        CarUi.Layout badge=CarUi.compactLayout(actual,1,false,false);
        assertNotNull(badge);assertTrue(badge.bounds.right<1090);
        visible.set(12,12,1178,448);
        assertEquals("A new callback can expand the area again",visible,CarUi.overlayViewport(visible,stable,1190,460));
        assertEquals("Earlier frames retain their own geometry",new Rect(420,90,1090,448),actual);
    }
    @Test public void emptyOrUnavailableVisibleAreaCannotReappearAsAFullSurfaceOverlay() {
        Rect stable=new Rect(12,12,1178,448);
        assertTrue(CarUi.overlayViewport(new Rect(),stable,1190,460).isEmpty());
        assertTrue(CarUi.overlayViewport(null,null,1190,460).isEmpty());
        assertEquals(stable,CarUi.overlayViewport(null,stable,1190,460));
        assertTrue(CarUi.overlayViewport(new Rect(1300,0,1400,400),stable,1190,460).isEmpty());
        assertTrue(CarUi.overlayViewport(stable,null,0,460).isEmpty());
    }
    @Test public void overlayBoundsAreClippedOnceInSurfaceCoordinatesWithoutMutatingHostRects() {
        Rect host=new Rect(-20,-10,1300,500);
        assertEquals(new Rect(0,0,1190,460),CarUi.overlayViewport(host,null,1190,460));
        assertEquals(new Rect(-20,-10,1300,500),host);
    }

    @Test public void nativeSelectionAndDisabledCarKeepTheOriginalRenderer() {
        Canvas canvas=new Canvas(Bitmap.createBitmap(260,220,Bitmap.Config.ARGB_8888));
        assertFalse(CarUi.compact(new Object(),canvas));
        TelemetryRuntime.select("com.waze");TelemetryRuntime.setCarEnabled(false);
        assertFalse(CarUi.compact(new Object(),canvas));
    }
    @Test public void clusterFitsHostInsetsAndSplitDashboardAreas() {
        for(Rect safe:new Rect[]{new Rect(48,36,296,208),new Rect(400,80,620,300),
                new Rect(600,80,790,260),new Rect(24,32,1256,688)}) {
            CarUi.Layout layout=CarUi.compactLayout(safe,1,true,true);
            assertNotNull(layout);
            assertTrue(layout.bounds.left>=safe.left);
            assertTrue(layout.bounds.top>=safe.top);
            assertTrue(layout.bounds.right<=safe.right);
            assertTrue(layout.bounds.bottom<=safe.bottom);
            assertTrue(layout.bar.left>=layout.bounds.left);
            assertTrue(layout.bar.right<=layout.bounds.right);
            assertTrue(layout.bar.bottom<layout.pair.top);
            assertTrue(layout.bar.height()<=4);
        }
    }
    @Test public void extremelySmallHostAreaDoesNotCoverItsControls() {
        assertNull(CarUi.compactLayout(new Rect(0,0,80,60),1,true,true));
        CarUi.Layout speedOnly=CarUi.compactLayout(new Rect(0,0,180,100),1,false,false);
        assertNotNull(speedOnly);assertNull(speedOnly.bar);
    }
    @Test public void mapTextContainsOnlyAverageUnitsProgressAndDistance() {
        assertEquals("Ø90 km/h",CarUi.averageText(90d,false));
        assertEquals("Ø56 mph",CarUi.averageText(90d,true));
        assertEquals("≈35%",CarUi.progressText(35d,true));
        assertEquals("35%",CarUi.progressText(35d,false));
        assertEquals("1.2km",CarUi.distanceText(1200,false));
        assertEquals("0.7mi",CarUi.distanceText(1200,true));
        assertEquals("Ø— km/h",CarUi.averageText(null,false));
        assertEquals("Ø0 km/h",CarUi.averageText(0d,false));
    }
    @Test public void partialSemanticsRemainInTheModelAfterCompactFormatting() {
        TelemetryStore.View view=zone();
        assertTrue(view.partialAverage);assertTrue(view.progressEstimated);
        assertEquals("Ø90 km/h",CarUi.averageText(view.averageKmh,false));
        assertTrue(view.partialAverage);assertTrue(view.progressEstimated);
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void badgeStaysInsideTheSafeAreaAndNeverMutatesNativePaints() {
        Bitmap bitmap=Bitmap.createBitmap(260,220,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        CarUi.Layout layout=CarUi.compactLayout(new Rect(20,20,240,200),1,true,false);
        Paint number=new Paint(Paint.ANTI_ALIAS_FLAG),unit=new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint limit=new Paint(Paint.ANTI_ALIAS_FLAG),disc=new Paint(Paint.ANTI_ALIAS_FLAG),ring=new Paint(Paint.ANTI_ALIAS_FLAG);
        number.setTextSize(9);number.setColor(Color.MAGENTA);ring.setColor(0xffd32f2f);
        CarUi.renderCompact(canvas,layout,"0",100d,zone(),"",false,false,number,unit,limit,disc,ring);
        assertEquals(9,number.getTextSize(),0);assertEquals(Color.MAGENTA,number.getColor());
        assertTrue(Color.alpha(bitmap.getPixel((int)layout.bounds.left+3,(int)layout.bounds.top+3))<255);
        assertEquals("The combined badge has a light background",Color.WHITE,
                bitmap.getPixel((int)layout.pair.centerX(),(int)layout.pair.top+6));
        assertEquals("The limit sign is on the left of the current speed",0xffd32f2f,
                bitmap.getPixel((int)layout.pair.left+34,(int)layout.pair.top+4));
        assertTrue(layout.bar.bottom<layout.pair.top);
        assertEquals(0xff1f6feb,bitmap.getPixel((int)(layout.bar.left+layout.bar.width()*.1),(int)layout.bar.centerY()));
        assertEquals(0xffc5cbd0,bitmap.getPixel((int)(layout.bar.left+layout.bar.width()*.8),(int)layout.bar.centerY()));
        for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++) {
            if(x<layout.bounds.left||x>layout.bounds.right||y<layout.bounds.top||y>layout.bounds.bottom)
                assertEquals("draw outside host cluster",0,bitmap.getPixel(x,y));
        }
    }
    private static TelemetryStore.View zone() {
        TelemetryStore store=new TelemetryStore();store.select("com.waze");
        Map<String,Object> packet=TelemetryStoreTest.packet("com.waze","test",1,5000);
        packet.put("zone_status","active");packet.put("zone_session",1L);packet.put("zone_segment_id","fixture");
        packet.put("zone_remaining_m",2000);packet.put("zone_progress_percent",30d);packet.put("zone_progress_estimated",true);
        packet.put("zone_observation_start_elapsed_ms",1000L);packet.put("zone_observed_distance_m",100);
        packet.put("zone_average_partial",true);packet.put("zone_average_speed_kmh",90d);
        assertTrue(store.accept(packet,"com.waze",5000));return store.view(5000);
    }
}
