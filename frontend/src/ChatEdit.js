import React, {useState, useEffect} from 'react';
import axios from 'axios'
import {makeStyles, withStyles} from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import Modal from '@material-ui/core/Modal';
import TextField from '@material-ui/core/TextField';
import {openEditModal, closeEditModal} from "./actions";
import {connect} from "react-redux";

const useStyles = makeStyles(theme => ({
    appHeader: {
        'background-color': '#282c34',
        display: 'flex',
        'flex-direction': 'column',
        'align-items': 'center',
        'justify-content': 'center',
        'font-size': 'calc(10px + 2vmin)',
        color: 'white',
        'word-wrap': 'break-word',
        'font-family': 'monospace',
    },
    root: {
        width: '100%',
        height: '100%',
        backgroundColor: theme.palette.background.paper,
    },
    fabAddButton: {
        position: 'fixed',
        zIndex: 1,
        bottom: 30,
        right: 30,
        margin: '0 auto',
    },
    scroller: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: "center",
    },
    paper: {
        position: 'absolute',
        width: 400,
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2),
    },
    confirm: {
        position: 'absolute',
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2),
    },
}));

function getModalStyle() {
    const top = 50;
    const left = 50;

    return {
        top: `${top}%`,
        left: `${left}%`,
        transform: `translate(-${top}%, -${left}%)`,
    };
}

function ChatEdit({ currentState, dispatch, passEditDto, fetchData }) {
    // state
    const [modalStyle] = useState(getModalStyle);

    const [editDto, setEditDto] = useState(passEditDto);
    const [valid, setValid] = useState(true);

    const handleCloseEditModal = () => {
        dispatch(closeEditModal());
    };

    const validString = s => {
        if (s) {
            return true
        } else {
            return false
        }
    };

    const validate = (dto) => {
        let v = validString(dto.name);
        //console.log("Valid? " + JSON.stringify(dto) + " : " + v);
        setValid(v)
    };

    const handleChangeName = event => {
        const dto = {...editDto, name: event.target.value};
        setEditDto(dto);
        validate(dto);
    };

    const onSave = (dto, event) => {
        (dto.id ? axios.put(`/api/chat`, dto) : axios.post(`/api/chat`, dto))
            .then(() => {
                // call on parent
                fetchData();
            })
            .then(() => {
                handleCloseEditModal();
            })
            .catch((error) => {
                // handle error
                console.log("Handling error on save", error.response);
            });
    };

    const classes = useStyles();

    return (
        <Modal
            aria-labelledby="simple-modal-title"
            aria-describedby="simple-modal-description"
            open={true}
            onClose={handleCloseEditModal}
        >
            <div style={modalStyle} className={classes.paper}>

                <Grid container
                      direction="column"
                      justify="center"
                      alignItems="stretch"
                      spacing={2} className="edit-modal">

                    <Grid item>
                        <span>{editDto.id ? 'Rename chat' : 'Create chat'}</span>
                    </Grid>
                    <Grid item container spacing={1} direction="column" justify="center"
                          alignItems="stretch">
                        <Grid item>
                            <TextField id="outlined-basic" label="Name" variant="outlined" fullWidth className="edit-modal-name"
                                       error={!valid} value={editDto.name} onChange={handleChangeName}/>
                        </Grid>

                    </Grid>
                    <Grid item container spacing={1}>
                        <Grid item>
                            <Button variant="contained" color="primary" disabled={!valid} className="edit-modal-save"
                                    onClick={(e) => onSave(editDto, e)}>
                                Save
                            </Button>
                        </Grid>
                        <Grid item>
                            <Button variant="contained" color="secondary" onClick={handleCloseEditModal} className="edit-modal-cancel">
                                Cancel
                            </Button>
                        </Grid>
                    </Grid>
                </Grid>
            </div>
        </Modal>
    );
}

const mapStateToProps = state => ({
    currentState: state
});

export default connect(
    mapStateToProps
)(ChatEdit);