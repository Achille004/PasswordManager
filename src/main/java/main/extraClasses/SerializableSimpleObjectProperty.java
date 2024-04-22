package main.extraClasses;

import java.io.IOException;
import java.io.Serializable;

import javafx.beans.property.SimpleObjectProperty;

public class SerializableSimpleObjectProperty<T> extends SimpleObjectProperty<T> implements Serializable {
    /**
     * The constructor of {@code ObjectProperty}
     */
    public SerializableSimpleObjectProperty() {
        super();
    }

    /**
     * The constructor of {@code ObjectProperty}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public SerializableSimpleObjectProperty(T initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ObjectProperty}
     *
     * @param bean
     *            the bean of this {@code ObjectProperty}
     * @param name
     *            the name of this {@code ObjectProperty}
     */
    public SerializableSimpleObjectProperty(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ObjectProperty}
     *
     * @param bean
     *            the bean of this {@code ObjectProperty}
     * @param name
     *            the name of this {@code ObjectProperty}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public SerializableSimpleObjectProperty(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }
    
    private final void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeObject(getValue());
    }

    @SuppressWarnings("unchecked")
    private final void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        setValue((T) stream.readObject());
    }
}
