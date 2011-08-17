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

package com.evolveum.midpoint.web.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author sleepwalker
 */
public class UserManagerImplMock implements UserManager {

	private static final long serialVersionUID = -6949750285264000739L;
	@Autowired
	AccountManagerImplMock accountManagerMock;
	@Autowired
	ResourceManagerImplMock resourceManagerMock;
	Map<String, GuiUserDto> userTypeList = new HashMap<String, GuiUserDto>();

	@Override
	public void delete(String oid) {
		userTypeList.remove(oid);
	}

	@Override
	public Collection<GuiUserDto> list() {
		return userTypeList.values();
	}

	public UserDto get(String oid) {
		GuiUserDto lookupUser = new GuiUserDto();

		for (GuiUserDto user : userTypeList.values()) {
			if (oid.equals(user.getOid())) {
				lookupUser = user;
			}
		}
		return lookupUser;
	}

	@Override
	public String add(GuiUserDto newObject) {
		userTypeList.clear();
		newObject.setOid(UUID.randomUUID().toString());
		userTypeList.put(newObject.getOid(), newObject);
		return newObject.getOid();
	}

	@Override
	public Set<PropertyChange> submit(GuiUserDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public GuiUserDto create() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public AccountShadowDto addAccount(UserDto userDto, String resourceOid) {
		AccountShadowDto accountDto = new AccountShadowDto();
		AccountShadowType accountType = new AccountShadowType();
		accountType.setAttributes(new Attributes());
		ResourceDto resourceDto = resourceManagerMock.get(resourceOid, new PropertyReferenceListType());
		accountType.setResource((ResourceType) resourceDto.getXmlObject());
		accountDto.setXmlObject(accountType);

		System.out.println("account Resource namespace " + accountDto.getResource().getNamespace());

		return accountDto;

	}

	@Override
	public GuiUserDto get(String oid, PropertyReferenceListType resolve) {

		System.out.println("user mock");
		System.out.println("wanted " + oid);
		System.out.println("in list " + userTypeList.get(oid).getOid());
		GuiUserDto userDto = null;
		for (GuiUserDto user : userTypeList.values()) {
			if (user.getOid().equals(oid)) {
				userDto = user;
			}
		}

		if (!resolve.getProperty().isEmpty()) {
			Collection<AccountShadowDto> accounts = accountManagerMock.list();
			Collection<GuiResourceDto> resources = resourceManagerMock.list();
			for (PropertyReferenceType property : resolve.getProperty()) {
				if (Utils.getPropertyName("Account").equals(
						(new XPathHolder(property.getProperty())).getXPath())) {
					for (AccountShadowDto acc : accounts) {
						if (acc.getOid().equals(userDto.getAccountRef().get(0).getOid())) {
							((UserType) userDto.getXmlObject()).getAccount().add(
									(AccountShadowType) acc.getXmlObject());
							System.out.println("acc res ref" + acc.getResourceRef().getOid());

						}
					}
				}

				if (Utils.getPropertyName("Resource").equals(
						(new XPathHolder(property.getProperty())).getXPath())) {
					for (ResourceDto res : resources) {
						System.out.println("res oid " + res.getOid());
						if (res.getOid().equals(userDto.getAccount().get(0).getResourceRef().getOid())) {
							AccountShadowType accountType = new AccountShadowType();
							accountType.setResource((ResourceType) res.getXmlObject());
							System.out.println("account type res " + accountType.getResource().getName());
							((AccountShadowType) (userDto.getAccount().get(0).getXmlObject()))
									.setResource((ResourceType) res.getXmlObject());
							// ((UserType)
							// guiUserDto.getXmlObject()).getAccount().set(0,
							// (AccountShadowType) accountType);

						}
					}
				}
			}
		}
		return userDto;
	}

	@Override
	public Collection<GuiUserDto> list(PagingType paging) {
		return userTypeList.values();
	}

	@Override
	public List<UserDto> search(QueryType search, PagingType paging) {
		// TODO Auto-generated method stub
		return null;
	}
}
