package com.ericwen229.topic;

import lombok.NonNull;
import org.ros.internal.message.Message;

public interface SubscriberHandler<T extends Message> {

	void accept(@NonNull T messsage);

}
