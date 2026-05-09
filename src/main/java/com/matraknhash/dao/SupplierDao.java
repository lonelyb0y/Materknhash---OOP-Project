package com.matraknhash.dao;

import com.matraknhash.model.Supplier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SupplierDao extends BaseDao<Supplier> {

    @Override protected String table() { return "suppliers"; }
    @Override protected String[] columns() {
        return new String[]{"name","phone","email","address"};
    }

    @Override protected Supplier extract(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address")
        );
    }

    @Override protected void bindInsert(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getPhone());
        ps.setString(3, s.getEmail());
        ps.setString(4, s.getAddress());
    }

    @Override protected void bindUpdate(PreparedStatement ps, Supplier s) throws SQLException {
        bindInsert(ps, s);
        ps.setInt(5, s.getId());
    }

    @Override protected int idOf(Supplier s) { return s.getId(); }
    @Override protected void setId(Supplier s, int id) { s.setId(id); }
}
