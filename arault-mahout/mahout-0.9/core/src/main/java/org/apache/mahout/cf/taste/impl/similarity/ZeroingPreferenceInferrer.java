package org.apache.mahout.cf.taste.impl.similarity;

import java.util.Collection;
import java.lang.UnsupportedOperationException;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;

/**
 * 
 * A pessimistic perference inferrer which assumes items for which the rating is unknown are disliked.
 * Return a zero rating whatever the user/item.
 *
 */
public final class ZeroingPreferenceInferrer implements PreferenceInferrer {
  
  @Override
  public float inferPreference(long userID, long itemID) {
    return (float) 0.0;
  }
  
  @Override
  public void refresh(Collection<Refreshable> alreadyRefreshed) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public String toString() {
    return "ZeroingPreferenceInferrer";
  }
  
}

