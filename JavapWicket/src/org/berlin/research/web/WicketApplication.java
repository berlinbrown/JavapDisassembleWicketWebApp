package org.berlin.research.web;

import org.apache.wicket.protocol.http.WebApplication;
import org.berlin.research.javap.TestViewRuntimeInfo;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run the Start class.
 *
 * org.berlin.research.web.WicketApplication
 */
public class WicketApplication extends WebApplication {

    /**
     * Constructor
     */
    public WicketApplication() {}

    /**
     * Initialize; This method is called after this application class is
     * constructed, and the wicket servlet is set.
     */
    @Override
    protected void init() {
        super.init();
        mountBookmarkablePage("/runtime", TestViewRuntimeInfo.class);
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class< HomePage > getHomePage() {
        return HomePage.class;
    }


} // End of class