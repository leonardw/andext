package io.github.leonardw.andext;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2014 Leonard Wu <leonard.wu92@alumni.ic.ac.uk>
 * https://github.com/leonardw/andext
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * An extension of {@code android.support.v4.app.FragmentStatePagerAdapter} to allow straight forward
 * locating of paged {@code Fragment} by position, via getFragment(position)
 *
 * This does not force instantiation of new {@code Fragment}s; instead, only returns it if one has already
 * been created through normal {@code PageAdapter} lifecycle.
 */
public abstract class TrackedFragmentStatePagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter {
	/**
	 * Default max number of {@code Fragment}s to hold in cache before a sweep is triggered.
	 */
	private static final int MAX_FRAGMENT_CACHE_SIZE = 10;

	/**
	 * Object of this type can be used to track the currently selected page in a ViewPager
	 */
	public interface PageTracker {
		public boolean isCurrentPosition(int position);
	}

	/**
	 * Each page object must implement this in order to be automatically assigned a PageTracker
	 * on creation
	 */
	public interface Pageable {
		public void setPageTracker(PageTracker pageTracker);
	}

	PageTracker mPageTracker;

	/**
	 * The entries count threshold, upon which to trigger a dead reference sweep
	 */
	private int mSweepThreshold = MAX_FRAGMENT_CACHE_SIZE;

	/**
	 * The lookup table between position and the corresponding {@code Fragment}.
	 * Values are {@code WeakReference}s to {@code Fragment}s what have been created previously.
	 * Typically, only up to 3 entries will hold valid references to active {@code Fragment}s at
	 * any one given time.
	 */
	private Map<Integer, WeakReference<Fragment>> mFragmentCache = new HashMap<Integer, WeakReference<Fragment>>();

	/**
	 * Constructor
	 *
	 * @param fm The {@code FragmentManager}
	 */
	public TrackedFragmentStatePagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		final Object item = super.instantiateItem(container, position);
		if (item instanceof Fragment && item != getFragment(position)) {
			if (item instanceof Pageable) {
				((Pageable)item).setPageTracker(mPageTracker);
			}
			mFragmentCache.put(position, new WeakReference<Fragment>((Fragment) item));
			sweep();
		}
		return item;
	}

	/**
	 * Get the {@code Fragment} at a given position
	 *
	 * This does not force instantiation of new {@code Fragment}s; instead, only returns it if one has already
	 * been created through normal {@code PageAdapter} lifecycle.
	 *
	 * @param position Position at which the {@code Fragment} is located
	 * @return The {@code Fragment} at the given position. Returns {@code null} if
	 * a {@code Fragment} does not exist or has already been destroyed through
	 * lifecycle events
	 */
	public Fragment getFragment(int position) {
		final WeakReference<Fragment> ref = mFragmentCache.get(position);
		return (ref == null) ? null : ref.get();
	}

	/**
	 * Set the number of {@code Fragment}s in cache before a sweep for dead references
	 * is triggered.
	 *
	 * This should be larger than 3 since the {@code PageAdapter} will typically hold
	 * at least 3 active {@code Fragments}. (In observation, up to 5 active Fragments
	 * are kept.)
	 *
	 * Set this to a higher number to reduce the frequency of housekeeping sweep.
	 *
	 * @param sweepThreshold The entries count threshold
	 */
	public void setSweepThreshold(int sweepThreshold) {
		this.mSweepThreshold = sweepThreshold;
	}

	/**
	 * Iterate through all entries in cache and remove any dead {@code WeakReference}s
	 * to {@code Fragment}s
	 */
	private void sweep() {
		if (mFragmentCache.size() > mSweepThreshold) {
			Iterator<Map.Entry<Integer, WeakReference<Fragment>>> it = mFragmentCache.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, WeakReference<Fragment>> entry = it.next();
				if (entry.getValue().get() == null) {
					it.remove();
				}
			}
		}
	}

	public void setPageTracker(PageTracker pageTracker) {
		this.mPageTracker = pageTracker;
	}
}
