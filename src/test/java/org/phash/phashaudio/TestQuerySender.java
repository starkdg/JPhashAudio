package org.phash.phashaudio;

import java.util.Arrays;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;

public class TestQuerySender {

    protected final String addr = "http://localhost:4005";

    protected QuerySender sender = null;

    @Test public void testCreateQuerySender(){
		assert(sender == null);
    }
}
