package org.magemello.sys.node;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Reloadable;


@Sources({"file:~/application.properties"})
public interface Configuration extends Config, Accessible, Reloadable {
}
