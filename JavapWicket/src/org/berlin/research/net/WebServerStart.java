/**
 * Copyright Berlin Brown
 * Simple web application for javap, disassemble java classes
 * View Java bytecode in assembly format.
 * 
 * Tested with Java6, Jetty, Wicket1.4.13
 * 
 * keywords: javap, java, java6, scala, jetty, asm, bytecode
 * 
 * Visit: http://localhost:7181/run/
 * 
 * Run application in Eclipse environment.
 */
package org.berlin.research.net;

import static java.lang.System.out;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/** 
 * Basic Jetty Start Server.
 * Complete web application for Javap operations. 
 */
public class WebServerStart {
    
    public static void main( String[] args ) throws Exception {
    	    	
    	System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
    	System.setProperty("javax.net.ssl.trustStore", "/java6/jre/lib/security/new2/cacerts");
    	
    	com.sun.net.ssl.internal.ssl.Provider p = new com.sun.net.ssl.internal.ssl.Provider(); 
    	java.security.Security.addProvider(p); 
    	  
        Logger logger = Logger.getLogger( WebServerStart.class );
        Server server = new Server();
        SocketConnector connector = new SocketConnector();
       
        connector.setMaxIdleTime( 1000 * 60 * 60 );
        connector.setSoLingerTime( -1 );
        connector.setPort( 7181 );
        server.setConnectors( new Connector[] { connector } );
        
        WebAppContext bb = new WebAppContext();
        bb.setServer( server );
        bb.setContextPath( "/" );        
        bb.setWar( "WebContent" );               
        server.addHandler( bb );        
        try {
            logger.debug( "RUNNING JETTY" );            
            server.start();            
            out.println(">>> Running on port : " + connector.getPort());            
            System.in.read();
            logger.debug( "STOPPING EMBEDDED JETTY SERVER" );
            server.stop();
            server.join();
        } catch ( Exception e ) {
            logger.error( "ERROR:", e );
            System.exit( 100 );
        }
    }
} // End of Class //
