package io.github.delanym.maven.notifier.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CompositeNotifierTest {

    @Mock
    private Notifier first;

    @Mock
    private Notifier second;

    @Test
    void sendsToAllDelegates() {
        CompositeNotifier composite = new CompositeNotifier(List.of(first, second));
        Notification notification = testNotification();

        composite.send(notification);

        Mockito.verify(first).send(notification);
        Mockito.verify(second).send(notification);
    }

    @Test
    void continuesSendingWhenOneDelegateFails() {
        CompositeNotifier composite = new CompositeNotifier(List.of(first, second));
        Notification notification = testNotification();
        Mockito.doThrow(new RuntimeException("boom")).when(first).send(notification);

        composite.send(notification);

        Mockito.verify(second).send(notification);
    }

    @Test
    void closesAllDelegatesEvenOnFailure() {
        CompositeNotifier composite = new CompositeNotifier(List.of(first, second));
        Mockito.doThrow(new RuntimeException("close failed")).when(first).close();

        composite.close();

        Mockito.verify(first).close();
        Mockito.verify(second).close();
    }

    @Test
    void isPersistentIfAnyDelegateIsPersistent() {
        Mockito.when(first.isPersistent()).thenReturn(false);
        Mockito.when(second.isPersistent()).thenReturn(true);
        CompositeNotifier composite = new CompositeNotifier(List.of(first, second));

        Assertions.assertThat(composite.isPersistent()).isTrue();
    }

    private Notification testNotification() {
        return Notification.builder()
                .title("Test")
                .message("Test message")
                .icon(Icon.of(BuildStatus.SUCCESS.iconUrl(), "test"))
                .build();
    }
}
