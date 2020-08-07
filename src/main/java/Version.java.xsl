<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="text" />
<xsl:template match="grotag-version">// Grotag - Amigaguide viewer, converter and pretty printer.
// Copyright (C) 2008 Thomas Aglassinger
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
package net.sf.grotag.common;

/**
 *  Grotag version information. Generated using "ant version".
 *
 * @author    Thomas Aglassinger
 */
public interface Version
{
    String COPYRIGHT = "Copyright 2008, 2016 Thomas Aglassinger";

    /**
     *  Release date using format YYYY-MM-DD.
     */
    String DATE = "<xsl:value-of select="date" />";

    /**
     *  The x.RELEASE.x part in the VERSION_TAG.
     */
    int RELEASE = <xsl:value-of select="release" />;

    /**
     *  The x.x.REVISON part in the VERSION_TAG.
     */
    int REVISION = <xsl:value-of select="revision" />;

    /**
     *  The VERSION.x.x part in the VERSION_TAG.
     */
    int VERSION = <xsl:value-of select="version" />;

    /**
     *  The full VERSION.RELEASE.REVISION text.
     */
    String VERSION_TAG = "" + VERSION + "." + RELEASE + "." + REVISION;
}

</xsl:template>
</xsl:stylesheet>

