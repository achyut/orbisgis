//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.17 at 12:33:03 PM CET 
//


package net.opengis.se._2_0.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SolidFillType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SolidFillType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/se/2.0/core}FillType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/se/2.0/core}Color" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/se/2.0/core}Opacity" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SolidFillType", propOrder = {
    "color",
    "opacity"
})
public class SolidFillType
    extends FillType
{

    @XmlElement(name = "Color")
    protected ParameterValueType color;
    @XmlElement(name = "Opacity")
    protected ParameterValueType opacity;

    /**
     * Gets the value of the color property.
     * 
     * @return
     *     possible object is
     *     {@link ParameterValueType }
     *     
     */
    public ParameterValueType getColor() {
        return color;
    }

    /**
     * Sets the value of the color property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParameterValueType }
     *     
     */
    public void setColor(ParameterValueType value) {
        this.color = value;
    }

    /**
     * Gets the value of the opacity property.
     * 
     * @return
     *     possible object is
     *     {@link ParameterValueType }
     *     
     */
    public ParameterValueType getOpacity() {
        return opacity;
    }

    /**
     * Sets the value of the opacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParameterValueType }
     *     
     */
    public void setOpacity(ParameterValueType value) {
        this.opacity = value;
    }

}