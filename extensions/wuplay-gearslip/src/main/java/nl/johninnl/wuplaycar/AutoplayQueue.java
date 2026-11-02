package nl.johninnl.wuplaycar;
import java.util.*;
/** A bounded, cancellable selection run. Each retry rechecks the shared budget. */
public final class AutoplayQueue<T extends CarStreamPolicy.Candidate> {
 private final List<T> candidates;private int cursor,attempts;private boolean cancelled;
 public AutoplayQueue(List<T> candidates,String preferred){this.candidates=CarStreamPolicy.ranked(candidates,preferred);}
 public T next(){while(!cancelled&&attempts<3&&cursor<candidates.size()){T value=candidates.get(cursor++);if(value.direct()&&CarStreamPolicy.eligible(value.bitrate())){attempts++;return value;}}return null;}
 public void cancel(){cancelled=true;}
 public int attempts(){return attempts;}
}
