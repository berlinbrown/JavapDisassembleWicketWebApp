package org.berlin.research.web;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.berlin.research.javap.TestViewRuntimeInfo;

/**
 * Homepage.
 */
public class HomePage extends WebPage {
    
    private final static Logger LOGGER = Logger.getLogger(HomePage.class);

    /**
     * Default constructor
     */
    public HomePage() {
        this( null );
    }
    
    /**
     * Constructor that is invoked when page is invoked without a session.
     * 
     * @param parameters Page parameters
     */
    public HomePage( final PageParameters parameters ) {       
        super( parameters );
        this.add(new Link<Object>("link") {                     
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LOGGER.info("Clicking on link");
                setResponsePage(TestViewRuntimeInfo.class);
            } 
        });
    }
       
} // End of the Class //