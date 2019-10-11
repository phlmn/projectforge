/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.fibu.kost;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.projectforge.business.fibu.KostFormatter;


/**
 * Kost2Bridge for hibernate search: Kostenträger kann à la 6.201.57 gesucht werden.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HibernateSearchKost2Bridge implements FieldBridge
{
  /**
   * @see org.hibernate.search.bridge.FieldBridge#set(java.lang.String, java.lang.Object, org.apache.lucene.document.Document,
   *      org.hibernate.search.bridge.LuceneOptions)
   */
  public void set(final String name, final Object value, final Document document, final LuceneOptions luceneOptions)
  {
    final Kost2DO kost2 = (Kost2DO) value;
    final StringBuilder buf = new StringBuilder();
    buf.append(KostFormatter.format(kost2));
    buf.append(' ');
    buf.append(KostFormatter.format(kost2, true));
    luceneOptions.addFieldToDocument(name, buf.toString(), document);
  }
}
