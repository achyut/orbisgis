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

package org.gdms.sql.engine.commands.join

import org.gdms.data.schema.DefaultMetadata
import org.gdms.sql.engine.GdmSQLPredef._
import org.gdms.sql.engine.commands.Command
import org.gdms.sql.engine.commands.Row
import org.gdms.sql.engine.commands.SQLMetadata

/**
 * This command performs a basic block-nested loop cross join for 2 tables.
 *
 * @author Antoine Gourlay
 * @since 0.1
 */
class LoopJoinCommand extends Command {
  protected final def doWork(r: Iterator[RowStream]) = {
    //This method just concats two 'rows' into one
    val doReduce = (a: Row, b: Row) => Row(a ++ b)

    val left = r.next
    val right = r.next.toSeq
    // for every batch in left, we take avery batch in right and apply
    // the doReduce function within the Promise objects
    
    left flatMap (p => right map (doReduce(p, _)))
  }

  override def getMetadata = {
    val d = new DefaultMetadata()
    children foreach { c => addAndRename(d, c.getMetadata) }
    SQLMetadata("", d)
  }
  
  private def addAndRename(d: DefaultMetadata, m: SQLMetadata) {
    // fields are given an internal name 'field$table'
    // for reference by expressions upper in the query tree
    m.getFieldNames.zipWithIndex foreach { n =>
        d.addField(n._1 + "$" + m.table,m.getFieldType(n._2))
    }
  }
}
