package com.xebialabs.overcast.host;

import org.junit.Before;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.junit.Test;

import com.xebialabs.overcast.command.Command;
import com.xebialabs.overcast.command.CommandProcessor;
import com.xebialabs.overcast.command.CommandResponse;
import com.xebialabs.overcast.command.NonZeroCodeException;
import com.xebialabs.overcast.support.vagrant.VagrantDriver;
import com.xebialabs.overcast.support.virtualbox.VirtualboxDriver;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static com.xebialabs.overcast.host.CachedVagrantCloudHost.EXPIRATION_TAG_PROPERTY_KEY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CachedVagrantCloudHostTest {

    public static final String SOME_SHA = "ef65erfds-i-am-git-SHA-bk34hg";

    public static final String SOME_OTHER_SHA = "uygw4-i-am-git-SHA-k34h";

    @Mock
    private VagrantDriver vagrantDriver;

    @Mock
    private VirtualboxDriver virtualboxDriver;

    @Mock
    private CommandProcessor commandProcessor;

    private CachedVagrantCloudHost cloudHost;

    private Command myCommand;

    @Before
    public void setUp() {
        initMocks(this);
        myCommand = Command.fromString("my-command");
        cloudHost = new CachedVagrantCloudHost("myvm", "127.0.0.1", myCommand, vagrantDriver, virtualboxDriver, commandProcessor);
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailIfExpirationCommandFails() {
        when(commandProcessor.run(myCommand)).thenThrow(new NonZeroCodeException(myCommand, new CommandResponse(2, "", "")));
        cloudHost.setup();
    }

    @Test
    public void shouldVagrantUpAndSetTagIfVmDoesNotExist() {
        // Happy expiration command execution
        when(commandProcessor.run(myCommand)).thenReturn(new CommandResponse(0, "", SOME_SHA));

        // Happy vagrant upping
        when(vagrantDriver.status("myvm")).thenReturn(new CommandResponse(0, "", "not created"));
        when(vagrantDriver.doVagrant("myvm", "up")).thenReturn(new CommandResponse(0, "", ""));

        cloudHost.setup();

        InOrder inOrder = inOrder(vagrantDriver, virtualboxDriver);
        inOrder.verify(vagrantDriver).doVagrant("myvm", "up");
        inOrder.verify(virtualboxDriver).setExtraData("myvm", EXPIRATION_TAG_PROPERTY_KEY, SOME_SHA);
    }

    @Test
    public void shouldBootVmIfTagExists() {
        throw new NotImplementedException();
    }

    @Test
    public void shouldPowerOffWhenTagExists() {

        when(virtualboxDriver.getExtraData("myvm", EXPIRATION_TAG_PROPERTY_KEY)).thenReturn(SOME_OTHER_SHA);

        cloudHost.teardown();

        verify(virtualboxDriver).powerOff("myvm");

        // Tag is already set and we not gonna update it
        verify(commandProcessor, never()).run(myCommand);

        // We should do the stuff via VBoxManage only
        verify(vagrantDriver, never()).doVagrant(anyString(), anyString());
    }

    @Test
    public void shouldDestroyWithVagrantWhenTagDoesNotExist() {
        when(virtualboxDriver.getExtraData("myvm", EXPIRATION_TAG_PROPERTY_KEY)).thenReturn(null);

        cloudHost.teardown();

        verify(vagrantDriver).doVagrant("myvm", "destroy", "-f");
        verify(virtualboxDriver, never()).powerOff("myvm");
    }
}