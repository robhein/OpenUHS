package net.vhati.openuhs.core;


/**
 * Ancestor of subclasses representing secondary ids of UHSNodes.
 *
 * <p>Subclasses should override toString().</p>
 *
 * @see net.vhati.openuhs.core.UHSHotSpotNode.HotSpotMainImageId
 * @see net.vhati.openuhs.core.UHSParseContext#registerExtraId(int)
 */
public abstract class ExtraNodeId {
	private final int id;


	public ExtraNodeId( int id ) {
		this.id = id;
	}


	public int getId() {
		return id;
	}
}
