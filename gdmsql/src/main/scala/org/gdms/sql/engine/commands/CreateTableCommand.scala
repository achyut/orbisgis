/** OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information. OrbisGIS is
 * distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 *
 *
 * Team leader : Erwan BOCHER, scientific researcher,
 *
 * User support leader : Gwendall Petit, geomatic engineer.
 *
 * Previous computer developer : Pierre-Yves FADET, computer engineer, Thomas LEDUC,
 * scientific researcher, Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Maxence LAURENT, Antoine GOURLAY
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 *
 * or contact directly:
 * info@orbisgis.org
 */

package org.gdms.sql.engine.commands

import Row._
import org.gdms.sql.engine.GdmSQLPredef._

/**
 * This command creates a table with the name <tt>name</tt> from the result
 * of its first child (which has to be an OutputCommand).
 *
 * @author Antoine Gourlay
 * @since 0.1
 */
class CreateTableCommand(name: String) extends Command with OutputCommand {

  override def doPrepare = {
    // register the new source
    // this will throw an exception if a source with that name already exists
    dsf.getSourceManager.register(name, dsf.getResultFile)

  }

  protected final def doWork(r: Iterator[RowStream]) = {
    val o = children.head.asInstanceOf[OutputCommand]
    val ds = o.getResult

    // gdms is writing for us
    dsf.saveContents(name, ds)

    null
  }

  val getResult = null

  // no result
  override val getMetadata = null
}
