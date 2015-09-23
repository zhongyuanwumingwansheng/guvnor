/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.guvnor.structure.client.editors.repository.create;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.guvnor.structure.client.editors.repository.RepositoryPreferences;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.repositories.EnvironmentParameters;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryAlreadyExistsException;
import org.guvnor.structure.repositories.RepositoryService;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.extras.select.client.ui.Option;
import org.gwtbootstrap3.extras.select.client.ui.Select;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.IOCResolutionException;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.ext.widgets.common.client.common.popups.BaseModal;
import org.uberfire.ext.widgets.common.client.common.popups.errors.ErrorPopup;
import org.uberfire.ext.widgets.core.client.resources.i18n.CoreConstants;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

@Dependent
public class CreateRepositoryForm extends Composite implements HasCloseHandlers<CreateRepositoryForm> {

    interface CreateRepositoryFormBinder
            extends
            UiBinder<Widget, CreateRepositoryForm> {

    }

    private static CreateRepositoryFormBinder uiBinder = GWT.create( CreateRepositoryFormBinder.class );

    @Inject
    private Caller<RepositoryService> repositoryService;

    @Inject
    private Caller<OrganizationalUnitService> organizationalUnitService;

    @Inject
    private PlaceManager placeManager;

    @UiField
    FormGroup organizationalUnitGroup;

    @UiField
    HelpBlock organizationalUnitHelpBlock;

    @UiField
    Select organizationalUnitDropdown;

    @UiField
    FormGroup nameGroup;

    @UiField
    TextBox nameTextBox;

    @UiField
    HelpBlock nameHelpBlock;

    @UiField
    FormLabel ouLabel;

    @UiField
    BaseModal popup;

    private Map<String, OrganizationalUnit> availableOrganizationalUnits = new HashMap<String, OrganizationalUnit>();
    private boolean mandatoryOU = true;

    @PostConstruct
    public void init() {
        GWT.log( "create repo" );
        initWidget( uiBinder.createAndBindUi( this ) );

        ouLabel.setShowRequiredIndicator( isOUMandatory() );

        nameTextBox.addKeyPressHandler( new KeyPressHandler() {
            @Override
            public void onKeyPress( final KeyPressEvent event ) {
                nameGroup.setValidationState( ValidationState.NONE );
                nameHelpBlock.setText( "" );
            }
        } );
        //populate Organizational Units list box
        organizationalUnitService.call( new RemoteCallback<Collection<OrganizationalUnit>>() {
                                            @Override
                                            public void callback( Collection<OrganizationalUnit> organizationalUnits ) {
                                                final Option select = new Option();
                                                select.setText( CoreConstants.INSTANCE.SelectEntry() );
                                                organizationalUnitDropdown.add( select );
                                                if ( organizationalUnits != null && !organizationalUnits.isEmpty() ) {
                                                    for ( OrganizationalUnit organizationalUnit : organizationalUnits ) {
                                                        final Option option = new Option();
                                                        option.setText( organizationalUnit.getName() );
                                                        option.setValue( organizationalUnit.getName() );
                                                        organizationalUnitDropdown.add( option );
                                                        availableOrganizationalUnits.put( organizationalUnit.getName(),
                                                                                          organizationalUnit );
                                                    }
                                                }
                                                organizationalUnitDropdown.refresh();
                                            }
                                        },
                                        new ErrorCallback<Message>() {
                                            @Override
                                            public boolean error( final Message message,
                                                                  final Throwable throwable ) {
                                                Window.alert( CoreConstants.INSTANCE.CantLoadOrganizationalUnits() + " \n" + message.toString() );

                                                return false;
                                            }
                                        }
                                      ).getOrganizationalUnits();
    }

    private boolean isOUMandatory() {
        try {
            final IOCBeanDef<RepositoryPreferences> beanDef = IOC.getBeanManager().lookupBean( RepositoryPreferences.class );
            return beanDef == null || beanDef.getInstance().isOUMandatory();
        } catch ( IOCResolutionException exception ) {
        }
        return true;
    }

    @Override
    public HandlerRegistration addCloseHandler( final CloseHandler<CreateRepositoryForm> handler ) {
        return addHandler( handler, CloseEvent.getType() );
    }

    @UiHandler("create")
    public void onCreateClick( final ClickEvent e ) {

        final String organizationalUnit = organizationalUnitDropdown.getValue();
        if ( mandatoryOU && !availableOrganizationalUnits.containsKey( organizationalUnit ) ) {
            organizationalUnitGroup.setValidationState( ValidationState.ERROR );
            organizationalUnitHelpBlock.setText( CoreConstants.INSTANCE.OrganizationalUnitMandatory() );
            return;

        } else {
            organizationalUnitGroup.setValidationState( ValidationState.NONE );
        }

        if ( nameTextBox.getText() == null || nameTextBox.getText().trim().isEmpty() ) {
            nameGroup.setValidationState( ValidationState.ERROR );
            nameHelpBlock.setText( CoreConstants.INSTANCE.RepositoryNaneMandatory() );

        } else {
            repositoryService.call( new RemoteCallback<String>() {
                @Override
                public void callback( String normalizedName ) {
                    if ( !nameTextBox.getText().equals( normalizedName ) ) {
                        if ( !Window.confirm( CoreConstants.INSTANCE.RepositoryNameInvalid() + " \"" + normalizedName + "\". " + CoreConstants.INSTANCE.DoYouAgree() ) ) {
                            return;
                        }
                        nameTextBox.setText( normalizedName );
                    }

                    final String scheme = "git";
                    final String alias = nameTextBox.getText().trim();
                    final Map<String, Object> env = new HashMap<String, Object>( 3 );

                    //Cloned repositories are not managed by default.
                    env.put( EnvironmentParameters.MANAGED,
                             false );

                    repositoryService.call( new RemoteCallback<Repository>() {
                                                @Override
                                                public void callback( Repository o ) {
                                                    Window.alert( CoreConstants.INSTANCE.RepoCreationSuccess() );
                                                    hide();
                                                    placeManager.goTo( new DefaultPlaceRequest( "RepositoryEditor" ).addParameter( "alias", o.getAlias() ) );
                                                }
                                            },
                                            new ErrorCallback<Message>() {
                                                @Override
                                                public boolean error( final Message message,
                                                                      final Throwable throwable ) {
                                                    try {
                                                        throw throwable;
                                                    } catch ( RepositoryAlreadyExistsException ex ) {
                                                        ErrorPopup.showMessage( CoreConstants.INSTANCE.RepoAlreadyExists() );
                                                    } catch ( Throwable ex ) {
                                                        ErrorPopup.showMessage( CoreConstants.INSTANCE.RepoCreationFail() + " \n" + throwable.getMessage() );
                                                    }

                                                    return true;
                                                }
                                            }
                                          ).createRepository( availableOrganizationalUnits.get( organizationalUnit ),
                                                              scheme,
                                                              alias,
                                                              env );

                }
            } ).normalizeRepositoryName( nameTextBox.getText() );
        }
    }

    @UiHandler("cancel")
    public void onCancelClick( final ClickEvent e ) {
        hide();
    }

    public void hide() {
        popup.hide();
        CloseEvent.fire( this, this );
    }

    public void show() {
        popup.show();
    }

}