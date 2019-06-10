package repository

import fujitask.eff.Fujitask.Session

trait Transaction extends Session

trait ReadTransaction extends Transaction

trait ReadWriteTransaction extends ReadTransaction