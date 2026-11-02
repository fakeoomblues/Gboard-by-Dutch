package nl.johninnl.wuplaycar;
/** Fit video and captions inside the host's currently visible drawing area. */
public final class VideoViewport {
    public final int left,top,width,height,captionBottomMargin;
    private VideoViewport(int left,int top,int width,int height,int margin){this.left=left;this.top=top;this.width=width;this.height=height;captionBottomMargin=margin;}
    public static VideoViewport fit(int fullWidth,int fullHeight,int left,int top,int right,int bottom,float aspect){
        left=Math.max(0,left);top=Math.max(0,top);right=Math.min(fullWidth,right);bottom=Math.min(fullHeight,bottom);
        if(right<=left||bottom<=top){left=0;top=0;right=fullWidth;bottom=fullHeight;}
        if(!(aspect>0)||Float.isInfinite(aspect))aspect=16f/9f;
        int availableWidth=right-left,availableHeight=bottom-top,w=availableWidth,h=Math.round(w/aspect);
        if(h>availableHeight){h=availableHeight;w=Math.round(h*aspect);}
        return new VideoViewport(left+(availableWidth-w)/2,top+(availableHeight-h)/2,w,h,fullHeight-bottom+Math.max(16,availableHeight/12));
    }
}
