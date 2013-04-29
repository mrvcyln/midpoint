/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.internal;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.internal.dto.ResourceItemDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import javax.xml.namespace.QName;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author lazyman
 */
public class PageAccounts extends PageAdmin {

    private static final Trace LOGGER = TraceManager.getTrace(PageAccounts.class);

    private static final String DOT_CLASS = PageAccounts.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_EXPORT = DOT_CLASS + "export";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_RESOURCES = "resources";
    private static final String ID_LIST_SYNC_DETAILS = "listSyncDetails";
    private static final String ID_EXPORT = "export";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_CLEAR_EXPORT = "clearExport";
    private static final String ID_FILES_CONTAINER = "filesContainer";
    private static final String ID_FILES = "files";
    private static final String ID_FILE = "file";
    private static final String ID_FILE_NAME = "fileName";

    private IModel<List<ResourceItemDto>> resourcesModel;

    private IModel<ResourceItemDto> resourceModel = new Model<ResourceItemDto>();

    private File downloadFile;

    public PageAccounts() {
        resourcesModel = new LoadableModel<List<ResourceItemDto>>() {

            @Override
            protected List<ResourceItemDto> load() {
                return loadResources();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_MAIN_FORM);
        add(form);

        DropDownChoice<ResourceItemDto> resources = new DropDownChoice<ResourceItemDto>(
                ID_RESOURCES, resourceModel, resourcesModel,
                new IChoiceRenderer<ResourceItemDto>() {

                    @Override
                    public Object getDisplayValue(ResourceItemDto object) {
                        if (object == null) {
                            return "";
                        }

                        return object.getName();
                    }

                    @Override
                    public String getIdValue(ResourceItemDto object, int index) {
                        return Integer.toString(index);
                    }
                });
        form.add(resources);

        initLinks(form);

        final AjaxDownloadBehaviorFromFile ajaxDownloadBehavior = new AjaxDownloadBehaviorFromFile(true) {

            @Override
            protected File initFile() {
                return downloadFile;
            }
        };
        form.add(ajaxDownloadBehavior);

        WebMarkupContainer filesContainer = new WebMarkupContainer(ID_FILES_CONTAINER);
        filesContainer.setOutputMarkupId(true);
        form.add(filesContainer);

        ListView<String> files = new ListView<String>(ID_FILES, createFilesModel()) {

            @Override
            protected void populateItem(final ListItem<String> item) {
                AjaxLink file = new AjaxLink(ID_FILE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        downloadPerformed(target, item.getModelObject(), ajaxDownloadBehavior);
                    }
                };
                file.add(new Label(ID_FILE_NAME, item.getModelObject()));
                item.add(file);
            }
        };
        files.setRenderBodyOnly(true);
        filesContainer.add(files);

        ObjectDataProvider provider = new ObjectDataProvider(this, ShadowType.class);
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, createAccountsColumns());
        accounts.setOutputMarkupId(true);
        accounts.setItemsPerPage(50);
        form.add(accounts);
    }

    private IModel<List<String>> createFilesModel() {
        return new LoadableModel<List<String>>() {

            @Override
            protected List<String> load() {
                String[] filesArray;
                try {
                    MidpointConfiguration config = getMidpointConfiguration();
                    File exportFolder = new File(config.getMidpointHome() + "/export");
                    filesArray = exportFolder.list(new FilenameFilter() {

                        @Override
                        public boolean accept(java.io.File dir, String name) {
                            if (name.endsWith("xml")) {
                                return true;
                            }

                            return false;
                        }
                    });
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't list files", ex);
                    getSession().error("Couldn't list files, reason: " + ex.getMessage());

                    throw new RestartResponseException(PageDashboard.class);
                }

                if (filesArray == null) {
                    return new ArrayList<String>();
                }

                List<String> list =  Arrays.asList(filesArray);
                Collections.sort(list);

                return list;
            }
        };
    }

    private void initLinks(Form form) {
        AjaxSubmitLink listSyncDetails = new AjaxSubmitLink(ID_LIST_SYNC_DETAILS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listSyncDetailsPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(listSyncDetails);

        AjaxSubmitLink export = new AjaxSubmitLink(ID_EXPORT) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                exportPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(export);

        AjaxLink clearExport = new AjaxLink(ID_CLEAR_EXPORT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearExportPerformed(target);
            }
        };
        form.add(clearExport);
    }

    private List<IColumn> createAccountsColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.oid"),
                SelectableBean.F_VALUE + ".oid"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.name"),
                SelectableBean.F_VALUE + ".name"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationSituation"),
                SelectableBean.F_VALUE + ".synchronizationSituation"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationTimestamp"),
                SelectableBean.F_VALUE + ".synchronizationTimestamp"));

        return columns;
    }

    private ObjectFilter createResourceQueryFilter() {
        ResourceItemDto dto = resourceModel.getObject();
        if (dto == null) {
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        String oid = dto.getOid();
        try {
            RefFilter resourceRef = RefFilter.createReferenceEqual(ShadowType.class,
                    ShadowType.F_RESOURCE_REF, getPrismContext(), oid);

            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class, oid, null,
                    createSimpleTask(OPERATION_LOAD_ACCOUNTS), result);
            RefinedResourceSchema schema = RefinedResourceSchema.getRefinedSchema(resource);
            QName qname = null;
            for (RefinedObjectClassDefinition def : schema.getRefinedDefinitions(ShadowKindType.ACCOUNT)) {
                if (def.isDefault()) {
                    qname = def.getObjectClassDefinition().getTypeName();
                    break;
                }
            }

            if (qname == null) {
                error("Couldn't find default object class for resource '" + WebMiscUtil.getName(resource) + "'.");
                return null;
            }

            EqualsFilter objectClass = EqualsFilter.createEqual(ShadowType.class, getPrismContext(),
                    ShadowType.F_OBJECT_CLASS, qname);

            return AndFilter.createAnd(resourceRef, objectClass);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create query", ex);
            error("Couldn't create query, reason: " + ex.getMessage());
        } finally {
            result.recomputeStatus();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return null;
    }

    private List<ResourceItemDto> loadResources() {
        List<ResourceItemDto> resources = new ArrayList<ResourceItemDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> objects = getModelService().searchObjects(ResourceType.class, null, null,
                    createSimpleTask(OPERATION_LOAD_RESOURCES), result);

            if (objects != null) {
                for (PrismObject<ResourceType> object : objects) {
                    resources.add(new ResourceItemDto(object.getOid(), WebMiscUtil.getName(object)));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resources", ex);
            result.recordFatalError("Couldn't load resources, reason: " + ex.getMessage(), ex);
        } finally {
            if (result.isUnknown()) {
                result.recomputeStatus();
            }
        }

        Collections.sort(resources);

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            throw new RestartResponseException(PageDashboard.class);
        }

        return resources;
    }

    private void listSyncDetailsPerformed(AjaxRequestTarget target) {
        TablePanel table = (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS));
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        table.getDataTable().setCurrentPage(0);

        target.add(table, getFeedbackPanel());
    }

    private void exportPerformed(AjaxRequestTarget target) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = "accounts-" + format.format(new Date()) + ".xml";

        OperationResult result = new OperationResult(OPERATION_EXPORT);
        Writer writer = null;
        try {
            Task task = createSimpleTask(OPERATION_EXPORT);
            List<PrismObject<ShadowType>> shadows;
            int offset = 0;

            writer = createWriter(fileName);
            writeHeader(writer);
            do {
                shadows = getModelService().searchObjects(ShadowType.class, createQuery(offset), null, task, result);
                writeToFile(writer, shadows);

                if (!WebMiscUtil.isSuccessOrHandledError(result)) {
                    break;
                }
                offset += 20;
            } while (shadows != null && shadows.size() > 0);
            writeFooter(writer);

            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't export accounts", ex);
            error(getString("PageAccounts.exportException", ex.getMessage()));
        } finally {
            IOUtils.closeQuietly(writer);
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private Writer createWriter(String fileName) throws IOException {
        //todo improve!!!!

        MidpointConfiguration config = getMidpointConfiguration();
        File file = new File(config.getMidpointHome() + "/export/" + fileName);
        file.createNewFile();
        System.out.println(file.getAbsolutePath());

        return new OutputStreamWriter(new FileOutputStream(file), "utf-8");
    }

    private void writeHeader(Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<objects xmlns=\"");
        sb.append(SchemaConstantsGenerated.NS_COMMON);
        sb.append("\">\n");

        writer.write(sb.toString());
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</objects>\n");
    }

    private void writeToFile(Writer writer, List<PrismObject<ShadowType>> shadows) throws IOException,
            SchemaException {
        PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();

        for (PrismObject<ShadowType> shadow : shadows) {
            String object = domProcessor.serializeObjectToString(shadow);
            writer.write(object);
        }
    }

    private ObjectQuery createQuery(int offset) {
        ObjectPaging paging = ObjectPaging.createPaging(offset, 20);
        return ObjectQuery.createObjectQuery(createResourceQueryFilter(), paging);
    }

    private void clearExportPerformed(AjaxRequestTarget target) {
        try {
            MidpointConfiguration config = getMidpointConfiguration();
            File exportFolder = new File(config.getMidpointHome() + "/export");
            java.io.File[] files = exportFolder.listFiles();
            if (files == null) {
                return;
            }

            for (java.io.File file : files) {
                file.delete();
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete export files", ex);
            error("Couldn't delete export files, reason: " + ex.getMessage());
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private void downloadPerformed(AjaxRequestTarget target, String fileName,
                                   AjaxDownloadBehaviorFromFile downloadBehavior) {
        MidpointConfiguration config = getMidpointConfiguration();
        downloadFile = new File(config.getMidpointHome() + "/export/" + fileName);

        downloadBehavior.initiate(target);
    }
}
