package ir.TAHub.TAHub.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a TaPermission as plain text. With a plain text column (and no enum check in the database),
 * adding a new permission later needs no ALTER. A name that no longer exists is read as null and ignored.
 */
@Converter
public class TaPermissionConverter implements AttributeConverter<TaPermission, String> {

    @Override
    public String convertToDatabaseColumn(TaPermission permission) {
        return permission == null ? null : permission.name();
    }

    @Override
    public TaPermission convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TaPermission.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
