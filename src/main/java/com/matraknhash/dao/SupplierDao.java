package com.matraknhash.dao;

import com.matraknhash.model.Supplier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SupplierDao extends BaseDao<Supplier> {

    @Override protected String table() { return "suppliers"; }
    @Override protected String[] columns() {
        return new String[]{"name","phone","email","address","trusted"};
    }

    @Override protected Supplier extract(ResultSet rs) throws SQLException {
        Supplier s = new Supplier(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address")
        );
        s.setTrusted(rs.getInt("trusted") == 1);
        return s;
    }

    @Override protected void bindInsert(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getPhone());
        ps.setString(3, s.getEmail());
        ps.setString(4, s.getAddress());
        ps.setInt(5, s.isTrusted() ? 1 : 0);
    }

    @Override protected void bindUpdate(PreparedStatement ps, Supplier s) throws SQLException {
        bindInsert(ps, s);
        ps.setInt(6, s.getId());
    }

    @Override protected int idOf(Supplier s) { return s.getId(); }
    @Override protected void setId(Supplier s, int id) { s.setId(id); }
}
