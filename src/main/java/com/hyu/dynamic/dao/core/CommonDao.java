package com.hyu.dynamic.dao.core;

import java.io.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.*;

@NoRepositoryBean
public interface CommonDao<T, K extends Serializable> extends JpaRepository<T, K>, BaseDao<T, K>
{
}