package db

import (
	"database/sql"
	"errors"
	"fmt"
	"github.com/guregu/null"
	. "nkonev.name/chat/logger"
	"time"
)

type Message struct {
	Id             int64
	Text           string
	ChatId         int64
	OwnerId        int64
	CreateDateTime time.Time
	EditDateTime   null.Time
}

func (db *DB) GetMessages(chatId int64, userId int64, limit int, offset int, reverse bool) ([]*Message, error) {
	order := "asc"
	if reverse {
		order = "desc"
	}
	if rows, err := db.Query(fmt.Sprintf(`SELECT * FROM message WHERE chat_id IN ( SELECT chat_id FROM chat_participant WHERE user_id = $1 AND chat_id = $4 ) ORDER BY id %s LIMIT $2 OFFSET $3`, order), userId, limit, offset, chatId); err != nil {
		Logger.Errorf("Error during get chat rows %v", err)
		return nil, err
	} else {
		defer rows.Close()
		list := make([]*Message, 0)
		for rows.Next() {
			message := Message{}
			if err := rows.Scan(&message.Id, &message.Text, &message.ChatId, &message.OwnerId, &message.CreateDateTime, &message.EditDateTime); err != nil {
				Logger.Errorf("Error during scan message rows %v", err)
				return nil, err
			} else {
				list = append(list, &message)
			}
		}
		return list, nil
	}
}

func (tx *Tx) CreateMessage(m *Message) (id int64, createDatetime time.Time, editDatetime null.Time, err error) {
	if m == nil {
		return id, createDatetime, editDatetime, errors.New("message required")
	} else if m.Text == "" {
		return id, createDatetime, editDatetime, errors.New("text required")
	}

	res := tx.QueryRow(`INSERT INTO message (text, chat_id, owner_id) VALUES ($1, $2, $3) RETURNING id, create_date_time, edit_date_time`, m.Text, m.ChatId, m.OwnerId)
	if err := res.Scan(&id, &createDatetime, &editDatetime); err != nil {
		Logger.Errorf("Error during getting message id %v", err)
		return id, createDatetime, editDatetime, err
	}
	return id, createDatetime, editDatetime, nil
}

func (db *DB) CountMessages() (int64, error) {
	var count int64
	row := db.QueryRow("SELECT count(*) FROM message")
	err := row.Scan(&count)
	if err != nil {
		return 0, err
	} else {
		return count, nil
	}
}

func getMessageCommon(co CommonOperations, chatId int64, userId int64, messageId int64) (*Message, error) {
	row := co.QueryRow(`SELECT * FROM message m WHERE m.id = $1 AND chat_id in (SELECT chat_id FROM chat_participant WHERE user_id = $2 AND chat_id = $3)`, messageId, userId, chatId)
	message := Message{}
	err := row.Scan(&message.Id, &message.Text, &message.ChatId, &message.OwnerId, &message.CreateDateTime, &message.EditDateTime)
	if err != nil {
		if err == sql.ErrNoRows {
			// there were no rows, but otherwise no error occurred
			return nil, nil
		} else {
			Logger.Errorf("Error during get message row %v", err)
			return nil, err
		}
	} else {
		return &message, nil
	}
}

func (db *DB) GetMessage(chatId int64, userId int64, messageId int64) (*Message, error) {
	return getMessageCommon(db, chatId, userId, messageId)
}

func (tx *Tx) GetMessage(chatId int64, userId int64, messageId int64) (*Message, error) {
	return getMessageCommon(tx, chatId, userId, messageId)
}

func addMessageReadCommon(co CommonOperations, messageId, userId int64, chatId int64) error {
	_, err := co.Exec(`INSERT INTO message_read (last_message_id, user_id, chat_id) VALUES ($1, $2, $3) ON CONFLICT (user_id, chat_id) DO UPDATE SET last_message_id = $1  WHERE $1 > (SELECT MAX(last_message_id) FROM message_read WHERE user_id = $2 AND chat_id = $3)`, messageId, userId, chatId)
	return err
}

func (db *DB) AddMessageRead(messageId, userId int64, chatId int64) error {
	return addMessageReadCommon(db, messageId, userId, chatId)
}

func (tx *Tx) AddMessageRead(messageId, userId int64, chatId int64) error {
	return addMessageReadCommon(tx, messageId, userId, chatId)
}

func (tx *Tx) EditMessage(m *Message) error {
	if m == nil {
		return errors.New("message required")
	} else if m.Text == "" {
		return errors.New("text required")
	} else if m.Id == 0 {
		return errors.New("id required")
	}

	if _, err := tx.Exec(`UPDATE message SET text = $1, edit_date_time = utc_now() WHERE owner_id = $2 AND id = $3`, m.Text, m.OwnerId, m.Id); err != nil {
		Logger.Errorf("Error during editing message id %v", err)
		return err
	}
	return nil
}

func (db *DB) DeleteMessage(messageId int64, ownerId int64, chatId int64) error {
	if _, err := db.Exec(`DELETE FROM message WHERE id = $1 AND owner_id = $2 AND chat_id = $3`, messageId, ownerId, chatId); err != nil {
		Logger.Errorf("Error during deleting message id %v", err)
		return err
	}
	return nil
}

func getUnreadMessagesCommon(co CommonOperations, chatId int64, userId int64) (int64, error) {
	var count int64
	row := co.QueryRow("SELECT COUNT(*) FROM message WHERE chat_id = $1 AND id > COALESCE((SELECT last_message_id FROM message_read WHERE user_id = $2 AND chat_id = $1), 0)", chatId, userId)
	err := row.Scan(&count)
	if err != nil {
		return 0, err
	} else {
		return count, nil
	}
}

func (db *DB) GetUnreadMessages(chatId int64, userId int64) (int64, error) {
	return getUnreadMessagesCommon(db, chatId, userId)
}

func (tx *Tx) GetUnreadMessages(chatId int64, userId int64) (int64, error) {
	return getUnreadMessagesCommon(tx, chatId, userId)
}
