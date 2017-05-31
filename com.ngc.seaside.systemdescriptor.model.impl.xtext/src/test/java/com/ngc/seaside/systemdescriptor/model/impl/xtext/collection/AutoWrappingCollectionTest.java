package com.ngc.seaside.systemdescriptor.model.impl.xtext.collection;

import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.metadata.IMetadata;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.AbstractWrappedXtextTest;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.data.WrappedDataField;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.DataFieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.DataType;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoWrappingCollectionTest extends AbstractWrappedXtextTest {

   private AutoWrappingCollection<DataFieldDeclaration, IDataField> wrapped;

   private Data data;

   @Before
   public void setup() throws Throwable {
      data = factory().createData();

      wrapped = new AutoWrappingCollection<>(data.getFields(),
                                             f -> new WrappedDataField(resolver(), f),
                                             WrappedDataField::toXtext);

      DataFieldDeclaration field = factory().createDataFieldDeclaration();
      field.setName("field1");
      field.setType(DataType.STRING);
      data.getFields().add(field);
   }

   @Test
   public void testDoesWrapXtextList() throws Throwable {
      IDataField field = fieldWithName("field1");

      assertEquals("size not correct!",
                   1,
                   wrapped.size());
      assertFalse("isEmpty not correct!",
                  wrapped.isEmpty());
      assertTrue("contains not correct!",
                 wrapped.contains(field));

      Iterator<IDataField> i = wrapped.iterator();
      assertTrue("iterator.hasNext() not correct!",
                 i.hasNext());
      assertEquals("iterator.next() not correct!",
                   field.getName(),
                   i.next().getName());
      i.remove();
      assertFalse("iterator.remove() not correct!",
                  i.hasNext());

      assertTrue("did not return true if added!",
                 wrapped.add(field));
      assertFalse("add not correct!",
                  wrapped.isEmpty());
      assertTrue("did not return true if removed!",
                 wrapped.remove(field));
      assertTrue("remove not correct!",
                 wrapped.isEmpty());
   }

   private static IDataField fieldWithName(String name) {
      IDataField f = mock(IDataField.class);
      when(f.getName()).thenReturn(name);
      when(f.getType()).thenReturn(DataTypes.STRING);
      when(f.getMetadata()).thenReturn(IMetadata.EMPTY_METADATA);
      return f;
   }
}
