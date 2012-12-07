package one.xmpp.xml;

import javax.xml.namespace.QName;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public interface StanzaListener {

    public void onStanza(XmlElement xmlElement);

    public void onStreamBegin(XmlElement xmlElement);

    public void onStreamEnd(QName qName);
}
