/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.sync.action;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Vilo Repan
 */
public class ModifyPasswordAction extends BaseAction {

	private static final Trace trace = TraceManager.getTrace(ModifyPasswordAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		UserType userType = getUser(userOid, result);
		if (userType == null) {
			throw new SynchronizationException("Can't find user with oid '" + userOid + "'.");
		}

		if (!(change.getObjectChange() instanceof ObjectChangeModificationType)) {
			throw new SynchronizationException("Object change is not instacne of "
					+ ObjectChangeModificationType.class.getName());
		}

		PropertyModificationType pwd = getPasswordFromModification((ObjectChangeModificationType) change
				.getObjectChange());
		if (pwd == null) {
			trace.error("Couldn't find property modification with password change, returning.");
			return userOid;
		}

		try {
			ObjectModificationType changes = createPasswordModification(userType, pwd);

			// getModel().modifyObjectWithExclusion(changes,
			// change.getShadow().getOid(),
			// new Holder<OperationResultType>(resultType));
		} catch (Exception ex) {
			throw new SynchronizationException("Can't save user", ex);
		}

		return userOid;
	}

	private ObjectModificationType createPasswordModification(UserType user, PropertyModificationType password) {
		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(user.getOid());
		changes.getPropertyModification().add(password);

		return changes;
	}

	private PropertyModificationType getPasswordFromModification(ObjectChangeModificationType objectChange) {
		List<PropertyModificationType> list = objectChange.getObjectModification().getPropertyModification();
		for (PropertyModificationType propModification : list) {
			XPathHolder path = new XPathHolder(propModification.getPath());
			List<XPathSegment> segments = path.toSegments();
			if (segments.size() == 0 || !segments.get(0).getQName().equals(SchemaConstants.I_CREDENTIALS)) {
				continue;
			}

			PropertyModificationType.Value value = propModification.getValue();
			if (value == null) {
				continue;
			}
			List<Element> elements = value.getAny();
			for (Element element : elements) {
				if (SchemaConstants.I_PASSWORD.equals(new QName(element.getNamespaceURI(), element
						.getLocalName()))) {
					return propModification;
				}
			}
		}

		return null;
	}
}
