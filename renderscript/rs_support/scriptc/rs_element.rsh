/*
 * Copyright (C) 2012 The Android Open Source Project
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

/** @file rs_element.rsh
 *  \brief Element routines
 *
 *
 */

#ifndef __RS_ELEMENT_RSH__
#define __RS_ELEMENT_RSH__

// New API's
#if (defined(RS_VERSION) && (RS_VERSION >= 16))

/**
 * Elements could be simple, such as an int or a float, or a
 * structure with multiple sub elements, such as a collection of
 * floats, float2, float4. This function returns zero for simple
 * elements or the number of sub-elements otherwise.
 *
 * @param e element to get data from
 * @return number of sub-elements in this element
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetSubElementCount(rs_element e);

/**
 * For complex elements, this function will return the
 * sub-element at index
 *
 * @param e element to get data from
 * @param index index of the sub-element to return
 * @return sub-element in this element at given index
 */
extern rs_element __attribute__((overloadable))
    rsElementGetSubElement(rs_element, uint32_t index);

/**
 * For complex elements, this function will return the length of
 * sub-element name at index
 *
 * @param e element to get data from
 * @param index index of the sub-element to return
 * @return length of the sub-element name including the null
 *         terminator (size of buffer needed to write the name)
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetSubElementNameLength(rs_element e, uint32_t index);

/**
 * For complex elements, this function will return the
 * sub-element name at index
 *
 * @param e element to get data from
 * @param index index of the sub-element
 * @param name array to store the name into
 * @param nameLength length of the provided name array
 * @return number of characters actually written, excluding the
 *         null terminator
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetSubElementName(rs_element e, uint32_t index, char *name, uint32_t nameLength);

/**
 * For complex elements, some sub-elements could be statically
 * sized arrays. This function will return the array size for
 * sub-element at index
 *
 * @param e element to get data from
 * @param index index of the sub-element
 * @return array size of sub-element in this element at given
 *         index
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetSubElementArraySize(rs_element e, uint32_t index);

/**
 * This function specifies the location of a sub-element within
 * the element
 *
 * @param e element to get data from
 * @param index index of the sub-element
 * @return offset in bytes of sub-element in this element at
 *         given index
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetSubElementOffsetBytes(rs_element e, uint32_t index);

/**
 * Returns the size of element in bytes
 *
 * @param e element to get data from
 * @return total size of the element in bytes
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetBytesSize(rs_element e);

/**
 * Returns the element's data type
 *
 * @param e element to get data from
 * @return element's data type
 */
extern rs_data_type __attribute__((overloadable))
    rsElementGetDataType(rs_element e);

/**
 * Returns the element's data kind
 *
 * @param e element to get data from
 * @return element's data size
 */
extern rs_data_kind __attribute__((overloadable))
    rsElementGetDataKind(rs_element e);

/**
 * Returns the element's vector size
 *
 * @param e element to get data from
 * @return length of the element vector (for float2, float3,
 *         etc.)
 */
extern uint32_t __attribute__((overloadable))
    rsElementGetVectorSize(rs_element e);

#endif // (defined(RS_VERSION) && (RS_VERSION >= 16))

#endif // __RS_ELEMENT_RSH__

